package com.redhat.gss;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class ModuleInfo {
  private String name = null;
  private String slot = null;
  private List<URL> resources = new ArrayList<URL>();
  private List<String> dependencies = new ArrayList<String>();
  
  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getSlot() {
    return this.slot;
  }
  
  public void setSlot(String slot) {
    this.slot = slot;
  }
  
  public List<URL> getResources() {
    return Collections.unmodifiableList(this.resources);
  }
  
  public void setResources(List<URL> resources) {
    this.resources = resources;
  }

  public void addResource(URL url) {
    this.resources.add(url);
  }
  
  public List<String> getDependencies() {
    return Collections.unmodifiableList(this.dependencies);
  }
  
  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public void addDependency(String dep) {
    this.dependencies.add(dep);
  }
}
