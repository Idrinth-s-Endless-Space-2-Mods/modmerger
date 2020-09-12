package de.idrinth.endlessspace2.modmerger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
    var assets = new HashMap<String, Byte[]>();
    var data = new HashMap<String, HashMap<String, String>>();
    for (var mod : mods) {
      load(new File(workshopfolder.getAbsoluteFile().toString() + '/' + mod), assets, data);
    }
    write(data, assets);
  }

  private void load(File folder, HashMap<String, Byte[]> assets, HashMap<String, HashMap<String, String>> data) {
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
          //not parseable, do we care?
        }
      }
    }
  }
  private void collect(File folder, HashMap<String, Byte[]> assets)
  {
    
  }
  private void read(Node config, File folder, HashMap<String, HashMap<String, String>> data)
  {
    
  }
  private void write(HashMap<String, HashMap<String, String>> data, HashMap<String, Byte[]> assets)
  {
    
  }
}
