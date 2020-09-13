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
    write(data, assets, registry, modList);
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
            read(mod.item(0), folder, data, registry);
            collect(folder, assets, folder);
            return;
          }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
          //not parseable, do we care?
        }
      }
    }
  }
  private void collect(File folder, HashMap<String, File> assets, File root)
  {
    for (var file : folder.listFiles()) {
      if (file.isDirectory() && !file.getName().startsWith(".") && !file.getName().equals("Documentation")) {
        collect(file, assets, root);
      } else if (!file.isDirectory() && !file.getName().endsWith(".xml") && !file.getName().endsWith(".properties") && !file.getName().endsWith(".json") && !file.getName().endsWith(".txt") && !file.getName().startsWith(".") && !file.getName().equals("PublishedFile.Id")) {
        assets.put(file.getAbsolutePath().substring(root.getAbsolutePath().length()), file);
      }
    }
  }
  private void find(File folder, List<File> files, File root)
  {
    for (var file : folder.listFiles()) {
      if (file.isDirectory() && !file.getName().startsWith(".") && !file.getName().equals("Documentation")) {
        find(file, files, root);
      } else if (!file.isDirectory() && file.getName().endsWith(".xml")) {
        files.add(file);
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
  private void read(Node config, File folder, HashMap<String, HashMap<String, String>> data, HashMap<String, String> registry)
  {
    List<File> xmls = new ArrayList<>();
    find(folder, xmls, folder);
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
            var matcher = FileSystems.getDefault().getPathMatcher("glob:" + path.getTextContent());
            var target = new File(folder.getAbsolutePath() + "/" + path.getTextContent());
            for (var file : xmls) {
              if (target.getAbsoluteFile().toString().equals(file.getAbsoluteFile().toString()) || matcher.matches(file.getAbsoluteFile().toPath())) {
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
            var matcher = FileSystems.getDefault().getPathMatcher("glob:" + path.getTextContent());
            var target = new File(folder.getAbsolutePath() + "/" + path.getTextContent());
            for (var file : xmls) {
              if (target.getAbsoluteFile().toString().equals(file.getAbsoluteFile().toString()) || matcher.matches(file.getAbsoluteFile().toPath())) {
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
  private static String nodeToString(Node node, boolean noDeclaration) throws TransformerException {
    StringWriter sw = new StringWriter();

    Transformer t = TransformerFactory.newInstance().newTransformer();
    if (noDeclaration) {
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
    t.transform(new DOMSource(node), new StreamResult(sw));

    return sw.toString();
  }
  private void write(HashMap<String, HashMap<String, String>> data, HashMap<String, File> assets, HashMap<String, String> registry, String[] modList)
  {
    var name = "Merge" + LocalTime.now().toString();
    name = name.replaceAll(":", "-");
    name = name.replaceAll("\\.", "-");
    var target = new File(System.getProperty("user.home") + "/Documents/Endless space 2/Community/" + name);
    if (!target.isDirectory()) {
      target.mkdirs();
    }
    for (var path : assets.keySet()) {
      var out = new File(target.toString() + "/" + path);
      out.getParentFile().mkdirs();
      try {
        FileUtils.copyFile(assets.get(path), out);
      } catch (IOException ex) {
      }
    }
    for (String type : data.keySet()) {
      var out = new File(target.toString() + "/source/" + type + ".xml");
      out.getParentFile().mkdirs();
      try {
        out.createNewFile();
        FileUtils.write(out, "<Datatable>", Charset.defaultCharset(), true);
        for (String item : data.get(type).values()) {
          FileUtils.write(out, item, Charset.defaultCharset(), true);
        }
        FileUtils.write(out, "</Datatable>", Charset.defaultCharset(), true);
      } catch (IOException ex) {
      }
    }
    if (!registry.isEmpty()) {
      try {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        for (var path : registry.keySet()) {
          var list = new ArrayList<String>();
          list.addAll(Arrays.asList(path.split("/")));
          buildDom(doc, list, registry.get(path));
        }
        FileUtils.write(new File(target.toString()+"/source/Registry.xml"), nodeToString(doc, false), Charset.defaultCharset());
      } catch (TransformerException | IOException | ParserConfigurationException ex) {
      }
    }
    var modFile = new File(target.toString() + "/" + name + ".xml");
    try {
      var merge = IOUtils.toString(getClass().getResourceAsStream("Merge.xml"));
      merge = merge.replaceAll("%MergeTitle%", modName.getText());
      merge = merge.replaceAll("%MergeName%", modName.getText().replaceAll(" ", ""));
      var description = "";
      for (var mod : modList) {
        description += ";w:/"+mod;
      }
      merge = merge.replaceAll("%MergeDescription%", description.substring(1));
      FileUtils.write(modFile, merge, Charset.defaultCharset());
    } catch (IOException ex) {
    }
  }

  private void buildDom(Document doc, ArrayList<String> path, String value) {
    Node local = doc;
    for (var part : path) {
      var found = false;
      for(var i=0;i<local.getChildNodes().getLength();i++) {
        var loc = local.getChildNodes().item(i);
        if (part.equals(loc.getNodeName())) {
          local = loc;
          found = true;
          break;
        }
      }
      if (!found) {
        local.appendChild(doc.createElement(part));
        local = local.getChildNodes().item(local.getChildNodes().getLength()-1);
      }
    }
    local.setTextContent(value);
  }
}
