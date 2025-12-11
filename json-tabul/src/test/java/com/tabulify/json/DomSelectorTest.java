package com.tabulify.json;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.Test;

public class DomSelectorTest {


  @Test
  public void name() {
    // https://jsoup.org/cookbook/extracting-data/selector-syntax
    // https://jsoup.org/apidocs/org/jsoup/parser/XmlTreeBuilder.html

    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\">" +
      "<tests><test><id>xxx</id><status>xxx</status></test>" +
      "<test><id>xxx</id><status>xxx</status></test></tests>" +
      "</xml>";
    Document doc = Jsoup.parse(xml, Parser.xmlParser());
    for (Element e : doc.select("test")) {
      System.out.println(e);
    }
  }
}
