package net.bytle.html;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import net.bytle.fs.Fs;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Put all CSS properties inline
 * This is used mostly for Email
 *
 * <p>
 * Inspiration
 * <a href="https://stackoverflow.com/questions/4521557/automatically-convert-style-sheets-to-inline-style">...</a>
 * <a href="https://gist.github.com/moodysalem/69e2966834a1f79492a9">...</a>
 * <a href="https://github.com/eebbesen/brolson">...</a>
 */
public class CssInliner {

  static Logger LOGGER = LoggerFactory.getLogger(CssInliner.class);
  private static final String STYLE_NODE_NAME = "style";
  private static final String STYLE_ATTR_NAME = STYLE_NODE_NAME;
  private static final String CLASS_ATTR = "class";

  CSSOMParser parser = new CSSOMParser(new SACParserCSS3());

  private final Document document;

  /**
   * Because the CSS are inlined,
   * you don't need the class anymore
   * (but they are used for test to select, therefore the default
   * is not to remove)
   */
  private boolean removeClasses = false;

  /**
   * It was a way to validate the stylesheet, but it does not work.
   * <p>
   * False by default because they compile everything, and they don't take already common
   * supported css.
   * <p>
   * Example:
   * - <a href="https://github.com/bootstrap-email/bootstrap-email/discussions/206">...</a>
   * - no padding for button: <a href="https://www.caniemail.com/search/?s=padding">...</a>
   */
  private boolean useBootstrapEmail = false;

  public CssInliner(Document document) {
    this.document = document;
  }

  public CssInliner inline() {


    /**
     * Retrieve all element of the documents
     * and their related css properties from the style element
     */
    Map<Element, Map<String, String>> allElementsStyles = getCssPropertiesByElement();

    /**
     * Apply them inline
     */
    for (Map.Entry<Element, Map<String, String>> elementEntry : allElementsStyles.entrySet()) {
      Element element = elementEntry.getKey();
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<String, String> styleEntry : elementEntry.getValue().entrySet()) {
        builder
          .append(styleEntry.getKey())
          .append(":")
          .append(styleEntry.getValue())
          .append(";");
      }
      builder.append(element.attr(STYLE_ATTR_NAME));
      element.attr(STYLE_ATTR_NAME, builder.toString());

      /**
       * Remove the class that are no more needed ?
       */
      if (removeClasses) {
        element.removeAttr(CLASS_ATTR);
      }
    }
    return this;
  }

  private Map<Element, Map<String, String>> getCssPropertiesByElement() {

    /**
     * Retrieve all Style elements
     */
    CSSRuleList ruleList = getCssRulesAndDeleteStyleElement();

    /**
     * Build a map of the element
     * and of the CSS properties
     * This is done before changing the inline css style
     * to be able to conserve the importance of the
     * actual inline style properties (ie cascading)
     */
    Map<Element, Map<String, String>> cssPropertiesByElement = new HashMap<>();
    for (int ruleIndex = 0; ruleIndex < ruleList.getLength(); ruleIndex++) {

      CSSRule cssRule = ruleList.item(ruleIndex);
      /**
       * If it's a style rule
       */
      if (cssRule instanceof CSSStyleRule) {

        CSSStyleRule cssStyleRule = (CSSStyleRule) cssRule;
        String cssSelector = cssStyleRule.getSelectorText();

        Elements elements;
        try {
          elements = document.select(cssSelector);
        } catch (Exception e) {
          // :after css expression are not implemented
          // not an info level, not an error, ...
          LOGGER.debug("Css query error " + e.getMessage());
          continue;
        }
        for (Element element : elements) {
          Map<String, String> elementStyles = cssPropertiesByElement.computeIfAbsent(element, k -> new LinkedHashMap<>());
          CSSStyleDeclaration style = cssStyleRule.getStyle();
          for (int propertyIndex = 0; propertyIndex < style.getLength(); propertyIndex++) {
            String propertyName = style.item(propertyIndex);
            String propertyValue = style.getPropertyValue(propertyName);
            elementStyles.put(propertyName, propertyValue);
          }
        }
      }
    }
    return cssPropertiesByElement;

  }

  /**
   * Get all Css rules from the style element
   * and delete them
   *
   * @return the css rule list of the html
   */
  private CSSRuleList getCssRulesAndDeleteStyleElement() {

    StringBuilder styleContent = new StringBuilder();

    // Link Stylesheet node
    Elements styleSheetElements = document.select("link[rel=\"stylesheet\"]");
    for (Element linkElement : styleSheetElements) {
      String href = linkElement.attr("href");
      if (!href.equals("")) {
        URI url;
        try {
          url = (new URL(href)).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
          throw new RuntimeException(e);
        }
        Path stylesheetPath = Paths.get(url);
        String stylesheetString;
        if (useBootstrapEmail && stylesheetPath.getFileName().toString().equals("bootstrap.min.css")) {
          InputStream inputStream = CssInliner.class.getResourceAsStream("/bootstrap-email/bootstrap-email.css");
          stylesheetString = Fs.getInputStreamContent(inputStream);
        } else {
          try {
            stylesheetString = Fs.getFileContent(stylesheetPath);
          } catch (NoSuchFileException e) {
            throw new RuntimeException("The stylesheet (" + url + ") was not found");
          }
        }

        styleContent.append(stylesheetString);
      }
      linkElement.remove();
    }

    // Style node
    Elements styleElements = document.select(STYLE_NODE_NAME);

    for (Element styleElement : styleElements) {

      styleContent.append(styleElement
        .getAllElements()
        .get(0)
        .data()
      );
      styleElement.remove();

    }


    return getCssRulesFromString(styleContent.toString());
  }

  private CSSRuleList getCssRulesFromString(String styleRules) {
    InputSource source = new InputSource(new StringReader(styleRules));
    try {
      return parser.parseStyleSheet(source, null, null).getCssRules();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static CssInliner createFromStringDocument(String html) {
    Document doc = Jsoup.parse(html);
    return new CssInliner(doc);
  }

  @SuppressWarnings("unused")
  CssInliner setRemoveClasses(boolean b) {
    this.removeClasses = b;
    return this;
  }

  @Override
  public String toString() {
    return document.toString();
  }


  @SuppressWarnings("unused")
  public CssInliner useBootstrapEmail(boolean b) {
    this.useBootstrapEmail = b;
    return this;
  }
}
