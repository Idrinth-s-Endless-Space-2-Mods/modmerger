package de.idrinth.endlessspace2.modmerger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.HashMap;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class InitialController {

  @FXML
  private TextField endlessSpaceFolder;

  @FXML
  private void start() {
    //for testing assume we know what mods to merge - remember to subscribe
    String[] mods = {"2216068650", "932394338", "1224504743", "1263186686", "1301600120", "1316786885"};
    var workshopfolder = new File("C:\\Program Files (x86)\\Steam\\steamapps\\workshop\\content\\392110");
    var assets = new HashMap<String, File>();
    var data = new HashMap<String, HashMap<String, String>>();
    for (var mod : mods) {
      load(new File(workshopfolder.toString() + '/' + mod), assets, data);
    }
    write(data, assets);
  }

  private void load(File folder, HashMap<String, File> assets, HashMap<String, HashMap<String, String>> data)
  {
    for (var file : folder.listFiles()) {
      if (file.getName().endsWith(".xml")) {
        try {
          var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
          var mod = doc.getElementsByTagName("RuntimeModule");
          if (mod.getLength() == 1) {
            read(mod.item(0), folder, data);
            collect(folder, assets);
            return;
          }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
          System.err.println(49);
          System.err.println(ex);
          //not parseable, do we care?
        }
      }
    }
    System.out.println(assets);
  }
  private void collect(File folder, HashMap<String, File> assets)
  {
    collect(folder, assets, folder);
  }
  private void collect(File folder, HashMap<String, File> assets, File root)
  {
    for (var file : folder.listFiles()) {
      if (file.isDirectory() && !file.getName().startsWith(".") && !file.getName().equals("Documentation")) {
        collect(file, assets, root);
      } else if (!file.isDirectory() && !file.getName().endsWith(".xml") && !file.getName().startsWith(".") && !file.getName().equals("PublishedFile.Id")) {
        assets.put(file.getAbsolutePath().substring(root.getAbsolutePath().length()), file);
      }
    }
  }
  private void read(Node config, File folder, HashMap<String, HashMap<String, String>> data)
  {
    //
  }
  private void write(HashMap<String, HashMap<String, String>> data, HashMap<String, File> assets)
  {
    var name = "Merge" + LocalTime.now().toString();
    name = name.replaceAll(":", "-");
    name = name.replaceAll("\\.", "-");
    var target = new File("C:/users/Björn/Documents/Endless space 2/Community/" + name);
    if (!target.isDirectory()) {
      target.mkdirs();
    }
    for (var path : assets.keySet()) {
      var out = new File(target.toString() + "/" + path);
      out.getParentFile().mkdirs();
      try {
        FileUtils.copyFile(assets.get(path), out);
      } catch (IOException ex) {
          System.err.println(87);
          System.err.println(ex);
      }
    }
    for (String type : data.keySet()) {
      var out = new File(target.toString() + "/source/" + type + ".xml");
      out.getParentFile().mkdirs();
      try {
        out.createNewFile();
        for (String item : data.get(type).keySet()) {
          FileUtils.write(out, item, Charset.defaultCharset(), true);
        }
      } catch (IOException ex) {
          System.err.println(100);
          System.err.println(ex);
      }
    }
    var modFile = new File(target.toString() + "/" + name + ".xml");
    try {
      FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("Merge.xml"), modFile);
    } catch (IOException ex) {
          System.err.println(107);
          System.err.println(ex);
    }
  }
}
