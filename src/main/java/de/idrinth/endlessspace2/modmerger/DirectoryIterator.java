package de.idrinth.endlessspace2.modmerger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DirectoryIterator {
  private final Map<String, File> resources = new HashMap<>();
  private final Map<String, File> xmls = new HashMap<>();
  public void start(File directory) {
    xmls.clear();
    resources.clear();
    find(directory, directory);
  }
  private void find(File directory, File root) {

    for (var file : directory.listFiles()) {
      if (file.isDirectory()) {
        if (!file.getName().startsWith(".") && !file.getName().equals("Documentation")) {
          find(file, root);
        }
      } else if (!file.getName().endsWith(".bak") && !file.getName().endsWith(".properties") && !file.getName().endsWith(".json") && !file.getName().endsWith(".txt") && !file.getName().startsWith(".") && !file.getName().equals("PublishedFile.Id")) {
        var relative = file.getAbsolutePath().substring(root.getAbsolutePath().length());
        if (file.getName().endsWith(".xml")) {
          xmls.put(relative, file);
          continue;
        }
        resources.put(relative, file);
      }
    }
  }
  public Map<String, File> resources() {
    return resources;
  }
  public Map<String, File> xmls() {
    return xmls;
  }
}
