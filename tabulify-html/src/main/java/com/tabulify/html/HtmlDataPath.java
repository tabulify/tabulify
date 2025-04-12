package com.tabulify.html;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.fs.textfile.FsTextDataPathAttributes;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlDataPath extends FsTextDataPath {


  public static final MediaType HTML_MEDIA_TYPE = MediaTypes.TEXT_HTML;


  private Elements trElements;
  private boolean wasBuild = false;

  public HtmlDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path);

    this.addVariablesFromEnumAttributeClass(HtmlDataPathAttribute.class);

  }

  @Override
  public MediaType getMediaType() {
    return HTML_MEDIA_TYPE;
  }


  @Override
  public SelectStream getSelectStream() {
    buildHeaderAndSelectStreamIfNotDone();
    return new HtmlSelectStream(this, this.trElements);
  }

  private void buildHeaderAndSelectStreamIfNotDone() {

    if (this.wasBuild) {
      return;
    }
    this.wasBuild = true;

    /**
     * To not have a null pointer exception
     */
    if (this.relationDef == null) {
      this.relationDef = new RelationDefDefault(this);
    }

    Path nioPath = this.getAbsoluteNioPath();
    if (!Files.exists(nioPath)) {
      // A target file
      return;
    }

    String html = Strings.createFromPath(nioPath, this.getCharset()).toString();
    Document doc = Jsoup.parse(html);
    String selector = this.getTableSelectorOrDefault();
    Element table = doc.selectFirst(selector);
    if (table == null) {
      HtmlLogs.LOGS.warning("No table element was found with the selector " + selector + " in the resource (" + this + ")");
      relationDef.addColumn(this.getColumnName());
      return;
    }


    String rowSelector = this.getRowSelectorOrDefault();
    this.trElements = table.select(rowSelector);
    if (this.trElements.isEmpty()) {
      return;
    }
    Element firstTrElement = this.trElements.get(0);
    String headerSelector = this.getHeaderSelector();
    Elements headerElements = firstTrElement.select(headerSelector);
    if (!headerElements.isEmpty()) {
      this.trElements.remove(0);
      for (Element tdElement : headerElements) {
        relationDef.addColumn(tdElement.text());
      }
      return;
    }

    headerElements = table.select(headerSelector);
    if (!headerElements.isEmpty()) {
      throw new RuntimeException("The (" + headerSelector + ") headers element are not in the first (" + rowSelector + ") element of the table selector " + this.getTableSelector() + " in the resource (" + this + ")");
    }

    int elementsCount = firstTrElement.siblingElements().size();
    for (int i = 0; i < elementsCount; i++) {
      relationDef.addColumn(String.valueOf(i + 1));
    }

  }

  @Override
  public FsTextDataPath addVariable(String key, Object value) {


    HtmlDataPathAttribute htmlDataAttribute = null;
    try {
      htmlDataAttribute = Casts.cast(key, HtmlDataPathAttribute.class);
    } catch (Exception e) {
      super.addVariable(key, value);
    }

    try {
      Variable variable = getConnection().getTabular().createVariable(htmlDataAttribute, value);
      this.addVariable(variable);
    } catch (Exception e) {
      throw new RuntimeException("The variable (" + key + ") for the HTML path (" + this + ") could not be created with the value (" + value + ")", e);
    }

    return this;
  }

  public String getTableSelector() {

    try {
      return (String) this.getVariable(HtmlDataPathAttribute.TABLE_SELECTOR).getValue();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The TABLE_SELECTOR has already a default and should be added, it should not happen", e);
    }
  }

  public String getTableSelectorOrDefault() {

    try {
      return (String) this.getVariable(HtmlDataPathAttribute.TABLE_SELECTOR).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The TABLE_SELECTOR has already a default and should be added, it should not happen", e);
    }

  }

  @Override
  public RelationDef getOrCreateRelationDef() {

    buildHeaderAndSelectStreamIfNotDone();
    return this.relationDef;
  }


  public String getHeaderSelector() {

    try {
      return (String) this.getVariable(HtmlDataPathAttribute.HEADER_SELECTOR).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The HEADER_SELECTOR has already a default and should be added, it should not happen", e);
    }

  }

  public String getRowSelectorOrDefault() {

    try {
      return (String) this.getVariable(HtmlDataPathAttribute.ROW_SELECTOR).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The ROW_SELECTOR has already a default and should be added, it should not happen", e);
    }

  }

  public String getCellSelectorOrDefault() {

    try {
      return (String) this.getVariable(HtmlDataPathAttribute.CELL_SELECTOR).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The CELL_SELECTOR has already a default and should be added, it should not happen", e);
    }

  }


}
