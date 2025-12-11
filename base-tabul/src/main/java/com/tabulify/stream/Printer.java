package com.tabulify.stream;

import com.tabulify.spi.DataPath;
import com.tabulify.type.KeyNormalizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Printer {


  final PrintBuilder printBuilder;
  /**
   * The data path already printed
   * This is to support the {@link PrinterPrintFormat#STREAM} format
   */
  final Set<String> seenDataPathLogicalNameSet = new HashSet<>();
  final Map<String, Map<Integer, Integer>> seenDataPathColumnLengths = new HashMap<>();

  public Printer(PrintBuilder printBuilder) {
    this.printBuilder = printBuilder;
  }

  public static PrintBuilder builder() {
    return new PrintBuilder();
  }


  /**
   * Print the table output stream
   * align to the left
   *
   * @param dataPath - the select stream
   */
  public void print(DataPath dataPath) {

    assert dataPath != null : "Data Path is null";


    new PrinterExecution(this, dataPath)
      .print();


  }


  public static class PrintBuilder {

    /**
     * The separation line between print
     * (Ie footer)
     */
    public int footerSeparationLineCount = 1;

    /**
     * The symbol that is shown when a boolean is true
     */
    private String booleanTrueToken = "✓";
    /**
     * The symbol that is show when a boolean is false
     */
    private String booleanFalseToken = "";

    /**
     * The string shown when the value is null
     * We put it in between brace to show that it's printed
     * <null> is shown only when it's a diff
     */
    private String nullToken = null;

    /**
     * The string shown when the value is the empty string
     * <empty> is shown only when it's a diff
     */
    private String emptyStringToken = null;


    /**
     * The string shown when the value is a blank string
     * (ie only white space)
     * <blank> is shown only when it's a diff
     */
    private String blankStringToken = null;

    public PrintBuilder setBlankStringToken(String blankStringToken) {
      if (blankStringToken == null) {
        return this;
      }
      this.blankStringToken = blankStringToken;
      return this;
    }

    public PrintBuilder setEmptyStringToken(String emptyStringToken) {
      if (emptyStringToken != null) {
        return this;
      }
      this.emptyStringToken = emptyStringToken;
      return this;
    }


    public PrintBuilder setNullToken(String nullToken) {
      if (nullToken != null) {
        return this;
      }
      this.nullToken = nullToken;
      return this;
    }

    public void setHorizontalColumnMarginCount(int horizontalColumnMarginCount) {
      this.horizontalColumnMarginCount = horizontalColumnMarginCount;
    }

    /**
     * The number of spaces (ie margin between columns)
     */
    public int horizontalColumnMarginCount = 3;

    /**
     * Set the max sample size to calculate the column length
     */
    public PrintBuilder setMaxSampleSize(int maxSampleSize) {
      this.maxSampleSize = maxSampleSize;
      return this;
    }

    /**
     * Set the true token
     */
    public PrintBuilder setBooleanTrueToken(String booleanTrue) {
      this.booleanTrueToken = booleanTrue;
      return this;
    }

    /**
     * Set the true token
     */
    public PrintBuilder setBooleanFalseToken(String falseToken) {
      this.booleanFalseToken = falseToken;
      return this;
    }

    /**
     * the max sample size to calculate column length
     * to not going into a big wait time if the user print
     * a big table
     */
    public int maxSampleSize = 1000;
    boolean printNonVisibleCharacter = true;
    boolean printTableHeader = true;
    boolean printColumnHeaders = true;
    /**
     * A column name that contains the highlights colors
     * * r: record is red
     * * b: record is blue
     * * 2g,3b: cell 2 is green, cell 3 is blue
     */
    KeyNormalizer colorsColumnName;

    public PrintBuilder setFooterSeparationLineCount(int footerSeparationLineCount) {
      this.footerSeparationLineCount = footerSeparationLineCount;
      return this;
    }

    /**
     * Null because the default depends on the passed data path
     */
    private PrinterPrintFormat format = null;

    public Printer build() {
      return new Printer(this);
    }

    public PrintBuilder setFormat(PrinterPrintFormat printerPrintFormat) {
      this.format = printerPrintFormat;
      return this;
    }

    public PrintBuilder setPrintNonVisibleCharacter(boolean printNonVisibleCharacter) {
      this.printNonVisibleCharacter = printNonVisibleCharacter;
      return this;
    }

    public PrintBuilder setPrintTableHeader(boolean printTableHeader) {
      this.printTableHeader = printTableHeader;
      return this;
    }

    public PrintBuilder setPrintColumnHeaders(boolean printColumnHeaders) {
      this.printColumnHeaders = printColumnHeaders;
      return this;
    }

    public PrinterPrintFormat getFormat() {
      return this.format;
    }

    public PrintBuilder setColorsColumnName(String highlightColumnName) {
      if (highlightColumnName == null) {
        return this;
      }
      this.colorsColumnName = KeyNormalizer.createSafe(highlightColumnName);
      return this;
    }

    public String getBooleanTrueToken() {
      return booleanTrueToken;
    }

    public String getBooleanFalseToken() {
      return booleanFalseToken;
    }

    public String getNullToken() {
      return this.nullToken;
    }

    public String getEmptyStringToken() {
      return this.emptyStringToken;
    }

    public String getBlankStringToken() {
      return this.blankStringToken;
    }

  }
}
