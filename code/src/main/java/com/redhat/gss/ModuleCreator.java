package com.redhat.gss;

import com.redhat.gss.model.*;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import org.jboss.logging.Logger;
import static java.nio.file.StandardCopyOption.*;

public class ModuleCreator {
  private static final Logger log = Logger.getLogger(ModuleCreator.class);
  private static final ObjectFactory objectFactory = new ObjectFactory();

  public static void main(String[] args) throws Exception {
    ModuleInfo module = parseArgs(args);
    createModule(module);
  }

  public static ModuleInfo parseArgs(String[] args) throws Exception {
    if(args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
      printHelp();
      System.exit(0);
    }
    ModuleInfo module = new ModuleInfo();
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
        String[] jars = arg.split(":");
        for(String jar : jars) {
          module.addResource(new File(jar).toURL());
        }
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
        String[] deps = arg.split(",");
        for(String dep : deps) {
          module.addDependency(dep);
        }
      }
    }
    return module;
  }

  public static ModuleType fromModuleInfo(ModuleInfo module) {
    ModuleType moduleType = new ModuleType();
    moduleType.setName(module.getName());
    moduleType.setSlot(module.getSlot());
    setResources(module, moduleType);
    setDependencies(module, moduleType);
    return moduleType;
  }

  public static void setResources(ModuleInfo module, ModuleType moduleType) {
    ResourcesType resources = new ResourcesType();
    List<JAXBElement<?>> resourceList = resources.getResourceRootOrArtifactOrNativeArtifact();
    for(URL resourceUrl : module.getResources()) {
      ResourceType resourceType = new ResourceType();
      resourceType.setPath(resourceUrl.toString());
      JAXBElement<ResourceType> resource = objectFactory.createResourcesTypeResourceRoot(resourceType);
      resourceList.add(resource);
    }
    moduleType.setResources(resources);
  }

  public static void setDependencies(ModuleInfo module, ModuleType moduleType) {
    DependenciesType dependenciesType = new DependenciesType();
    for(String dep : module.getDependencies()) {
      ModuleDependencyType moduleDep = new ModuleDependencyType();
      String[] nameAndSlot = dep.split(":");
      if(nameAndSlot.length > 2) {
        log.fatalf("Invalid module dependency name: '%s'", dep);
        System.exit(1);
      }
      moduleDep.setName(nameAndSlot[0]);
      if(nameAndSlot.length == 2) {
        moduleDep.setSlot(nameAndSlot[1]);
      }
      dependenciesType.getModuleOrSystem().add(moduleDep);
      moduleType.setDependencies(dependenciesType);
    }
  }

  public static void createModule(ModuleInfo module) throws Exception {
    ModuleType moduleType = fromModuleInfo(module);
    createModule(moduleType);
  }

  public static void createModule(ModuleType moduleType) throws Exception {
    File dir = getModuleDir(moduleType);
    copyResources(dir, moduleType);
    marshalModuleXml(dir, moduleType);
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
      URL url = new URL(resource.getPath());
      //TODO: Support maven artifact URLs
      if(! "file".equals(url.getProtocol())) {
        log.fatalf("URL protocol currently not supported: '%s'", url.getProtocol());
        System.exit(1);
      }
      File resourceFile = new File(url.getFile());
      FileInputStream fis = new FileInputStream(resourceFile);
      File newResourceFile = new File(dir, resourceFile.getName());
      Files.copy(fis, Paths.get(newResourceFile.getAbsolutePath()), REPLACE_EXISTING);
      fis.close();
      resource.setPath(resourceFile.getName());
    }
  }

  public static void printHelp() {
    System.out.println("JBoss Module Creator: Create basic JBoss Module dir structure and module.xml");
    System.out.println("Example usage: java -jar module-creator.jar \\");
    System.out.println("\t--name com.redhat.gss:main \\");
    System.out.println("\t--jars resource1.jar:resource2.jar \\");
    System.out.println("\t--deps javax.api,org.apache.cxf");
  }
}
