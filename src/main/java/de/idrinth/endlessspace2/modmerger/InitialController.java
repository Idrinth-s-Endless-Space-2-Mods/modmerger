package de.idrinth.endlessspace2.modmerger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

public class InitialController {

  @FXML
  private TextField steamFolder;
  @FXML
  private TextField mods;
  @FXML
  private TextField modName;
  @FXML
  private TextArea log;
  private final DirectoryIterator dir = new DirectoryIterator();
  private final DirectoryWriter writer = new DirectoryWriter();

  @FXML
  private void start() {
    String[] modList = mods.getText().split(",");
    var workshopfolder = new File(steamFolder.getText() + "/steamapps/workshop/content/392110");
    if (modList.length < 2) {
      var alert = new Alert(Alert.AlertType.ERROR, "To few mods given");
      alert.show();
      return;
    }
    if (!workshopfolder.isDirectory()) {
      var alert = new Alert(Alert.AlertType.ERROR, "Invalid Steam directory given");
      alert.show();
      return;
    }
    if (modName.getText().isBlank()) {
      var alert = new Alert(Alert.AlertType.ERROR, "No Modname given");
      alert.show();
      return;
    }
    log.setText("");
    var assets = new HashMap<String, File>();
    var data = new HashMap<String, HashMap<String, String>>();
    var registry = new HashMap<String, String>();
    
    for (var mod : modList) {
      load(new File(workshopfolder.toString() + '/' + mod), assets, data, registry);
    }
    writer.write(data, assets, registry, modList, modName.getText());
    var alert = new Alert(Alert.AlertType.INFORMATION, "Mods merged");
    alert.show();
  }
  private void writeLog(String message)
  {
    log.appendText(message + "\n");
  }

  private void load(File folder, HashMap<String, File> assets, HashMap<String, HashMap<String, String>> data, HashMap<String, String> registry)
  {
    for (var file : folder.listFiles()) {
      if (file.getName().endsWith(".xml")) {
        try {
          var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
          var mod = doc.getElementsByTagName("RuntimeModule");
          if (mod.getLength() == 1) {
            dir.start(folder);
            read(mod.item(0), folder, data, registry);
            assets.putAll(dir.resources());
            return;
          }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
          //not parseable, do we care?
        }
      }
    }
  }
  private void merge(Node node, HashMap<String, String> registry, String path, File folder)
  {
    if (node.hasChildNodes()) {
      if (!"#document".equals(node.getNodeName())) {
        path = path +"/"+ node.getNodeName();
        if (path.startsWith("/")) {
          path=path.substring(1);
        }
      }
      for (var i=0; i<node.getChildNodes().getLength(); i++) {
        merge(node.getChildNodes().item(i), registry, path, folder);
      }
      return;
    }
    if ("#text".equals(node.getNodeName()) && !node.getTextContent().isBlank()) {
      if (registry.containsKey(path) && !registry.get(path).equals(node.getTextContent())) {
        writeLog(path + " is overwritten by mod " + folder.getName());
      }
      registry.put(path, node.getTextContent());
    }
  }
  private boolean matches(File file, File root, PathMatcher glob)
  {
    var min = root.getAbsoluteFile().toPath().getNameCount();
    var max = file.getAbsoluteFile().toPath().getNameCount();
    return glob.matches(file.getAbsoluteFile().toPath().subpath(min, max));
  }
  private void read(Node config, File folder, HashMap<String, HashMap<String, String>> data, HashMap<String, String> registry)
  {
    for (var i = 0; i < config.getChildNodes().getLength(); i++) {
      var node = config.getChildNodes().item(i);
      if (!"Plugins".equals(node.getNodeName())) {
        continue;
      }
      for (var j = 0; j < node.getChildNodes().getLength(); j++) {
        var plugin = node.getChildNodes().item(j);
        if ("RegistryPlugin".equals(plugin.getNodeName())) {
          //the registry has to be merged...
          for (var k=0; k < plugin.getChildNodes().getLength(); k++) {
            var path = plugin.getChildNodes().item(k);
            if (!path.getNodeName().equals("FilePath")) {
              continue;
            }
            var glob = "glob:" + path.getTextContent().replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
            var matcher = FileSystems.getDefault().getPathMatcher(glob);
            var target = new File(folder.getAbsolutePath() + "/" + path.getTextContent()).getAbsoluteFile();
            for (var file : dir.xmls().values()) {
              if (target.toString().equals(file.getAbsoluteFile().toString()) || matches(file, folder, matcher)) {
                try {
                  var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
                  merge(doc, registry, "", folder);
                } catch (ParserConfigurationException | SAXException | IOException ex) {
                  //not parseable, do we care?
                }
              }
            }
          }
        } else if ("DatabasePlugin".equals(plugin.getNodeName())) {
          //these overwrite resources by name
          var type = plugin.getAttributes().getNamedItem("DataType").getTextContent();
          type = type.substring(0, type.indexOf(","));
          for (var k=0; k < plugin.getChildNodes().getLength(); k++) {
            var path = plugin.getChildNodes().item(k);
            if (!path.getNodeName().equals("FilePath")) {
              continue;
            }
            var glob = "glob:" + path.getTextContent().replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
            var matcher = FileSystems.getDefault().getPathMatcher(glob);
            var target = new File(folder.getAbsolutePath() + "/" + path.getTextContent()).getAbsoluteFile();
            for (var file : dir.xmls().values()) {
              if (target.toString().equals(file.getAbsoluteFile().toString()) || matches(file, folder, matcher)) {
                try {
                  var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
                  if ("Datatable".equals(doc.getFirstChild().getNodeName())) {
                    for (var l = 0; l<doc.getFirstChild().getChildNodes().getLength(); l++) {
                      var element = doc.getFirstChild().getChildNodes().item(l);
                      if (element.getAttributes() == null || element.getAttributes().getNamedItem("Name") == null) {
                        continue;
                      }
                      var name = element.getAttributes().getNamedItem("Name").getTextContent();
                      data.putIfAbsent(type, new HashMap<>());
                      if (data.get(type).containsKey(name)) {
                        writeLog(name + " of type " + type + " overwritten by mod " + folder.getName());
                      }
                      element.normalize();
                      data.get(type).put(name, nodeToString(element, true));
                    }
                  }
                } catch (TransformerException | ParserConfigurationException | SAXException | IOException ex) {
                  //not parseable, do we care?
                }
              }
            }
          }
        }
      }
    }
  }
  private String nodeToString(Node node, boolean noDeclaration) throws TransformerException {
    StringWriter sw = new StringWriter();

    Transformer t = TransformerFactory.newInstance().newTransformer();
    if (noDeclaration) {
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
    t.transform(new DOMSource(node), new StreamResult(sw));

    return sw.toString();
  }
}
