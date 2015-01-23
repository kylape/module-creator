package com.redhat.gss;

import com.redhat.gss.model.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import org.jboss.logging.Logger;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.Paths;
import java.io.FileInputStream;

public class ModuleCreator {
  private static final Logger log = Logger.getLogger(ModuleCreator.class);
  private static final ObjectFactory objectFactory = new ObjectFactory();

  public static void main(String[] args) throws Exception {
    ModuleType module = parseArgs(args);
    createModule(module);
  }

  public static ModuleType parseArgs(String[] args) throws Exception {
    ModuleType module = new ModuleType();
    String arg = null;
    for(int i=0; i < args.length; i++) {
      arg = args[i];
      if("--name".equals(arg)) {
        if(i == (args.length-1)) {
          log.fatalf("Expected argument after '%s'", arg);
          System.exit(1);
        }
        arg = args[++i];
        if(arg.startsWith("--")) {
          log.fatalf("Expected value argument after '%s'", args[i-1]);
          System.exit(1);
        }
        String[] nameAndSlot = arg.split(":");
        if(nameAndSlot.length > 2) {
          log.fatalf("Invalid module name: '%s'", arg);
          System.exit(1);
        }
        module.setName(nameAndSlot[0]);
        if(nameAndSlot.length == 2) {
          module.setSlot(nameAndSlot[1]);
        }
      } else if("--jars".equals(arg)) {
        if(i == (args.length-1)) {
          log.fatalf("Expected argument after '%s'", arg);
        }
        arg = args[++i];
        if(arg.startsWith("--")) {
          log.fatalf("Expected value argument after '%s'", args[i-1]);
          System.exit(1);
        }
        ResourcesType resources = new ResourcesType();
        List<JAXBElement<?>> resourceList = resources.getResourceRootOrArtifactOrNativeArtifact();
        String[] jars = arg.split(":");
        for(String jar : jars) {
          ResourceType resourceType = new ResourceType();
          resourceType.setPath(jar);
          JAXBElement<ResourceType> resource = objectFactory.createResourcesTypeResourceRoot(resourceType);
          resourceList.add(resource);
        }
        module.setResources(resources);
      } else if("--deps".equals(arg)) {
        if(i == (args.length-1)) {
          log.fatalf("Expected argument after '%s'", arg);
          System.exit(1);
        }
        arg = args[++i];
        if(arg.startsWith("--")) {
          log.fatalf("Expected value argument after '%s'", args[i-1]);
          System.exit(1);
        }
        DependenciesType dependenciesType = new DependenciesType();
        String[] deps = arg.split(",");
        for(String dep : deps) {
          ModuleDependencyType moduleDep = new ModuleDependencyType();
          String[] nameAndSlot = dep.split(":");
          if(nameAndSlot.length > 2) {
            log.fatalf("Invalid module dependency name: '%s'", arg);
            System.exit(1);
          }
          moduleDep.setName(nameAndSlot[0]);
          if(nameAndSlot.length == 2) {
            moduleDep.setSlot(nameAndSlot[1]);
          }
          dependenciesType.getModuleOrSystem().add(moduleDep);
          module.setDependencies(dependenciesType);
        }
      }
    }
    return module;
  }

  public static void createModule(ModuleType module) throws Exception {
    File dir = getModuleDir(module);
    marshalModuleXml(dir, module);
    copyResources(dir, module);
  }

  public static File getModuleDir(ModuleType module) throws Exception {
    String dirName = module.getName().replace('.', File.separatorChar);
    if(module.getSlot() != null) {
      dirName += File.separator + module.getSlot();
    } else {
      dirName += File.separator + "main";
    }
    File dir = new File(dirName);
    if(!dir.exists()) {
      boolean success = dir.mkdirs();
      if(!success) {
        log.fatalf("Failed to create module dir '%s'", dirName);
        System.exit(1);
      }
    }
    return dir;
  }

  public static void marshalModuleXml(File dir, ModuleType module) throws Exception {
    File file = new File(dir, "module.xml");
    JAXBContext jaxbContext = JAXBContext.newInstance("com.redhat.gss.model");
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    jaxbMarshaller.marshal(module, file);
  }

  public static void copyResources(File dir, ModuleType module) throws Exception {
    List<JAXBElement<?>> resourceList = module.getResources().getResourceRootOrArtifactOrNativeArtifact();
    for(JAXBElement<?> jbe : resourceList) {
      ResourceType resource = (ResourceType)jbe.getValue();
      File resourceFile = new File(resource.getPath());
      FileInputStream fis = new FileInputStream(resourceFile);
      File newResourceFile = new File(dir, resourceFile.getName());
      Files.copy(fis, Paths.get(newResourceFile.getAbsolutePath()), REPLACE_EXISTING);
      fis.close();
    }
  }
}
