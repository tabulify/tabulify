package net.bytle.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Grade a HTML page
 */
public class HtmlGrading {


  /**
   * Check if this is a legit HTML page
   */
  static public void grade(String html) throws HtmlStructureException {
    Document document = Jsoup.parse(html);
    String title = document.title();
    if (title == null) {
      throw new HtmlStructureException("The page has no title");
    }
    int bodyElements = document.body().getAllElements().size();
    if (bodyElements < 10) {
      throw new HtmlStructureException("The number of elements in the body is too small (" + bodyElements + ")");
    }
    int textLength = document.body().text().length();
    if (textLength < 50) {
      throw new HtmlStructureException("The text length is too small (" + textLength + ")");
    }
  }
}
