package com.jaredrummler.webscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoogleDesignWebScraper {

  public static final void main(String[] args) throws IOException {
    List<MaterialColorPalette> palettes = scrapeColors();
    File dir = new File("colors");
    dir.mkdirs();
    colorsToXml(palettes, new File(dir, "material_colors.xml"));
    colorsToJava(palettes, new File(dir, "MaterialColors.java"));
  }

  private static List<MaterialColorPalette> scrapeColors() throws IOException {
    List<MaterialColorPalette> palettes = new ArrayList<>();

    Document document = Jsoup.connect("http://www.google.com/design/spec/style/color.html").get();
    Elements elements = document.select("li.color");

    MaterialColorPalette palette = null;

    for (Element element : elements) {

      if (element.classNames().contains("main-color")) {
        String name = element.text().substring(0, element.text().indexOf(" 500 "));
        palette = new MaterialColorPalette(name);
        palettes.add(palette);
        continue;
      }

      String text = element.text();
      if (!text.contains("#")) {
        continue;
      }

      String[] arr = text.split("#");

      switch (arr[0].toLowerCase()) {
        case "black":
        case "white":
          break;
        default:
          String color = String.format("#%08X", (0xFFFFFFFF & parseColor("#" + arr[1])));
          String name = palette.name.replaceAll(" ", "_") + "_" + arr[0].replaceAll(" ", "_");
          palette.colors.put(name, color);
      }
    }

    return palettes;
  }

  private static void colorsToXml(List<MaterialColorPalette> palettes, File destination)
      throws IOException {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    xml.append("<resources>\n");
    for (MaterialColorPalette palette : palettes) {
      xml.append("    <!-- " + palette.name + " -->\n");
      for (Map.Entry<String, String> entry : palette.colors.entrySet()) {
        xml.append("    ");
        xml.append(String.format("<color name=\"%s\">%s</color>",
            entry.getKey().toLowerCase(), entry.getValue()));
        xml.append('\n');
      }
    }
    xml.append("\n</resources>");
    System.out.println(xml.toString());
    Files.write(Paths.get(destination.getAbsolutePath()), xml.toString().getBytes());
  }

  private static void colorsToJava(List<MaterialColorPalette> palettes, File destination)
      throws IOException {
    StringBuilder xml = new StringBuilder();
    xml.append("public class " + destination.getName().replaceFirst("[.][^.]+$", "") + " {\n\n");
    for (MaterialColorPalette palette : palettes) {
      xml.append("    /* " + palette.name + " */\n");
      for (Map.Entry<String, String> entry : palette.colors.entrySet()) {
        xml.append("    ");
        xml.append(String.format("public static final int %s = %s;",
            entry.getKey().toUpperCase(), entry.getValue().replace("#", "0x")));
        xml.append('\n');
      }
    }
    xml.append("\n}");
    System.out.println(xml.toString());
    Files.write(Paths.get(destination.getAbsolutePath()), xml.toString().getBytes());
  }

  private static int parseColor(String colorString) {
    long color = Long.parseLong(colorString.substring(1), 16);
    if (colorString.length() == 7) {
      // Set the alpha value
      color |= 0x00000000ff000000;
    } else if (colorString.length() != 9) {
      throw new IllegalArgumentException("Unknown color");
    }
    return (int) color;
  }

  private static class MaterialColorPalette {

    final String name;

    final Map<String, String> colors = new LinkedHashMap<>();

    public MaterialColorPalette(String name) {
      this.name = name;
    }
  }

}
