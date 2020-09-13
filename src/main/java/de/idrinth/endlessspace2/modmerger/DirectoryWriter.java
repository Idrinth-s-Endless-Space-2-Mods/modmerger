package de.idrinth.endlessspace2.modmerger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class DirectoryWriter {
  
  private String nodeToString(Node node, boolean noDeclaration) throws TransformerException {
    StringWriter sw = new StringWriter();

    Transformer t = TransformerFactory.newInstance().newTransformer();
    if (noDeclaration) {
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
    t.transform(new DOMSource(node), new StreamResult(sw));

    return sw.toString();
  }
  public void write(HashMap<String, HashMap<String, Node>> data, HashMap<String, File> assets, HashMap<String, String> registry, String[] modList, String modName)
  {
    var target = buildModDir(modName);
    copyAssets(target, assets);
    System.out.println(data.size());
    writeXmls(target, data);
    writeRegistry(target, registry);
    writeModFile(target, modList, modName);
  }
  private File buildModDir(String modName)
  {
    var name = "Merge" + modName + LocalTime.now().toString();
    name = name.replaceAll(":", "-");
    name = name.replaceAll("\\.", "-");
    var target = new File(System.getProperty("user.home") + "/Documents/Endless space 2/Community/" + name);
    if (!target.isDirectory()) {
      target.mkdirs();
    }
    return target;
  }
  private void writeXmls(File target, HashMap<String, HashMap<String, Node>> data)
  {
    for (String type : data.keySet()) {
      System.out.println(type);
      var out = new File(target.toString() + "/source/" + type + ".xml");
      out.getParentFile().mkdirs();
      try {
        out.createNewFile();
        FileUtils.write(out, "<Datatable>", Charset.defaultCharset(), true);
        for (var item : data.get(type).values()) {
          FileUtils.write(out, nodeToString(item, true), Charset.defaultCharset(), true);
        }
        FileUtils.write(out, "</Datatable>", Charset.defaultCharset(), true);
      } catch (IOException | TransformerException ex) {
      }
    }
  }
  private void copyAssets(File target, HashMap<String, File> assets)
  {
    for (var path : assets.keySet()) {
      var out = new File(target.toString() + "/" + path);
      out.getParentFile().mkdirs();
      try {
        FileUtils.copyFile(assets.get(path), out);
      } catch (IOException ex) {
      }
    }
  }
  private void writeModFile(File target, String[] mods, String name)
  {
    var modFile = new File(target.toString() + "/" + name + ".xml");
    try {
      var merge = IOUtils.toString(getClass().getResourceAsStream("Merge.xml"));
      merge = merge.replaceAll("%MergeTitle%", name);
      merge = merge.replaceAll("%MergeName%", name.replaceAll(" ", ""));
      var description = "";
      for (var mod : mods) {
        description += ";w:/"+mod;
      }
      merge = merge.replaceAll("%MergeDescription%", description.substring(1));
      FileUtils.write(modFile, merge, Charset.defaultCharset());
    } catch (IOException ex) {
    }
  }
  private void writeRegistry(File target, HashMap<String, String> registry)
  {
    if (!registry.isEmpty()) {
      return;
    }
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
