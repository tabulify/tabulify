package net.bytle.html;

import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.type.DnsName;
import net.bytle.type.UriEnhanced;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Grade a HTML page
 */
public class HtmlGrading {


  /**
   * Check if this is a legit HTML page
   * for a specific domain
   */
  static public void grade(String html, DnsName apexDomainNameAsString) throws HtmlStructureException {
    if (html == null) {
      throw new HtmlStructureException("The HTML is empty (null, no body)");
    }
    Document document = Jsoup.parse(html);
    String title = document.title();
    if (title.isEmpty()) {
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

    /**
     * The ratio of first party vs third party link should be bigger than 1
     * Default page of installation
     * have only third part link
     * <p>
     * Example Cyberpanel
     * http://mail.backlinksgenerator.in/
     * with the following page:
     * <a href="http://cyberpanel.net">CyberPanel</a>
     * <a href="https://community.cyberpanel.net/">Forums</a>
     * <a href="https://community.cyberpanel.net/docs">Documentation</a>
     */
    Elements links = document.select("a");
    int internalLink = 0;
    int externalLink = 0;
    for (Element link : links) {
      String href = link.attr("href");
      if (href.isEmpty()) {
        continue;
      }
      UriEnhanced hrefUri;
      try {
        hrefUri = UriEnhanced.createFromString(href);
      } catch (IllegalStructure e) {
        continue;
      }
      try {
        DnsName hrefHost = hrefUri.getHost();
        if (hrefHost.getApexName().equals(apexDomainNameAsString.getApexName())) {
          internalLink++;
        } else {
          externalLink++;
        }
      } catch (NotFoundException e) {
        // no host, the url is relative
        internalLink++;
      }
    }
    if (externalLink > internalLink) {
      throw new HtmlStructureException("Too much external links (" + externalLink + ") compared to internal links (" + internalLink + ")");
    }
  }

}
