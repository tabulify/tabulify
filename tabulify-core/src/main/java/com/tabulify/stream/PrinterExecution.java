package com.tabulify.stream;

import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Strings;
import net.bytle.type.time.Timestamp;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.stream.PrinterPrintFormat.*;

/**
 * An execution object to scope the objects value
 */
public class PrinterExecution {


  private final Printer.PrintBuilder printBuilder;
  private final PrinterPrintFormat actualFormat;
  private final boolean seenDataPath;
  private ColumnDef<?> colorsColumnDef;
  private final DataPath dataPath;
  private final Map<Integer, Integer> columnPositionLengthMap;
  private final String blankStringToken;
  private final String emptyStringToken;
  private final String nullToken;
  private Boolean columnPositionLengthMapChanged = false;
  private final Printer printer;
  private RelationDef runtimeRelationDef;

  /**
   * Executable are executed when a {@link DataPath#getSelectStream()}
   * We can't execute it twice (open it twice), this insert stream permits to store the result temporarily
   */
  private InsertStream executableInsertStream;

  public PrinterExecution(Printer printer, DataPath dataPath) {


    this.printBuilder = printer.printBuilder;
    this.dataPath = dataPath;

    /**
     * Aesthetic: Print the header if it's not a text file
     * A conditional on the number of column is not relevant as we can have
     * legitimate structure with only one column such as a generator
     */
    PrinterPrintFormat tempActualFormat = this.printBuilder.getFormat();
    if (tempActualFormat == null) {
      if (dataPath.getClass().equals(FsTextDataPath.class)) {
        tempActualFormat = PIPE;
      } else {
        tempActualFormat = DEFAULT;
      }
    }
    actualFormat = tempActualFormat;

    boolean tempSeenDataPath = false;
    String dataPathLogicalName = dataPath.getLogicalName();
    if (actualFormat == STREAM) {
      if (printer.seenDataPathLogicalNameSet.contains(dataPathLogicalName)) {
        tempSeenDataPath = true;
      } else {
        printer.seenDataPathLogicalNameSet.add(dataPathLogicalName);
      }
    }
    seenDataPath = tempSeenDataPath;
    this.printer = printer;

    columnPositionLengthMap = this.getColumnPositionLengthMapAndDetectColorColumnIfAny();

    /**
     * Token for non-visible value
     * By default are shown only with color (ie diff report)
     */
    String blankStringToken1 = printBuilder.getBlankStringToken();
    if (blankStringToken1 == null) {
      if (this.colorsColumnDef != null) {
        blankStringToken1 = "<blank>";
      } else {
        // null value is printed as null by default
        blankStringToken1 = "";
      }
    }
    this.blankStringToken = blankStringToken1;

    String emptyStringToken1 = printBuilder.getEmptyStringToken();
    if (emptyStringToken1 == null) {
      if (this.colorsColumnDef != null) {
        emptyStringToken1 = "<empty>";
      } else {
        // null value is printed as null by default
        emptyStringToken1 = "";
      }
    }
    this.emptyStringToken = emptyStringToken1;

    String nullToken1 = printBuilder.getNullToken();
    if (nullToken1 == null) {
      if (this.colorsColumnDef != null) {
        nullToken1 = "<empty>";
      } else {
        // null value is printed as null by default
        nullToken1 = "";
      }
    }
    this.nullToken = nullToken1;

  }

  /**
   * Should we print the table header
   * not the column header, the table name and the comment
   */
  private boolean getPrintTableHeader(PrinterPrintFormat format, boolean seenDataPath) {
    if (format == STREAM) {
      return !seenDataPath;
    }
    if (format == PIPE) {
      return false;
    }
    return this.printBuilder.printTableHeader;
  }

  /**
   * Do we print non-visible character such as EOL
   */
  private boolean getPrintNonVisibleCharacter() {

    if (actualFormat == PIPE) {
      return false;
    }
    return this.printBuilder.printNonVisibleCharacter;

  }

  private String getPrintedString(Object value, ColumnDef<?> columnDef) {
    String string = getString(value, columnDef);
    // When using the output:
    // * in a pipe, we don't want to see the EOL
    // * in an attribute info, we want to see them
    if (!getPrintNonVisibleCharacter()) {
      return string;
    }
    // Make the non-visible character visible
    return Strings.createFromString(string).toPrintableCharacter();
  }

  private String getString(Object value, ColumnDef<?> columnDef) {
    if (value == null) {
      return this.nullToken;
    }
    if (value instanceof Boolean) {
      return ((Boolean) value) ? this.printBuilder.getBooleanTrueToken() : this.printBuilder.getBooleanFalseToken();
    }
    if (value instanceof Class<?>) {
      return ((Class<?>) value).getName();
    }
    if (value.getClass().equals(Date.class)) {
      // java.sql object (ie java.sql.Date/Time).toString() print the good format but not java.util.Date
      // we should not receive
      if (columnDef.getRelationDef().getDataPath().getConnection().getTabular().isIdeEnv()) {
        throw new InternalException("We should get a java.sql.Date or java.sql.Timestamp, not a java.util.Date");
      }
      return Timestamp.createFromDate((Date) value).toSqlTimestamp().toString();
    }
    if (Collection.class.isAssignableFrom(value.getClass())) {
      // Collections (json string?)
      throw new InternalException("The value of the column (" + columnDef + ") is a collection. Collections are not yet supported.");
    }
    if (value instanceof Map) {
      // Map (json string?)
      throw new InternalException("The value of the column (" + columnDef + ") is a map. A map is not yet supported.");
    }
    if (value.getClass().isArray()) {
      // Array (json string?)
      throw new InternalException("The value of the column (" + columnDef + ") is a array. A array is not yet supported.");
    }
    if (value instanceof String) {
      if (((String) value).isEmpty()) {
        return this.emptyStringToken;
      }
      if (((String) value).isBlank()) {
        return this.blankStringToken;
      }
    }
    return value.toString();


  }

  /**
   * Scanning the values:
   * * to determine the max length
   * * to verify that the number of values is the same as the number of columns
   */
  private Map<Integer, Integer> getColumnPositionLengthMapAndDetectColorColumnIfAny() {

    Map<Integer, Integer> columnPositionLengthMap = new HashMap<>();
    int maxSampleSize = this.printBuilder.maxSampleSize;
    if (dataPath.isRuntime()) {
      /**
       * an executable is executed before select, we can't execute it twice
       * so we insert all data in {@link #executableInsertStream}
       */
      maxSampleSize = Integer.MAX_VALUE;
    }
    int sampleCounter = 0;


    try (SelectStream dataPathSelectStream = getSelectStream(dataPath)) {

      this.runtimeRelationDef = dataPathSelectStream.getRuntimeRelationDef();

      /**
       * An executable is executed at runtime
       * We open the data path 2 times, one to get the max length {@link #getColumnPositionLengthMapAndDetectColorColumnIfAny()}
       * and one to {@link #print}. We don't want to execute it twice
       * We store then the data in another data path
       * We suppose that the cardinality is low
       * We do it at runtime time, ie selectStream otherwise there is no data def
       */
      if (this.dataPath.isRuntime() && executableInsertStream == null) {
        executableInsertStream = dataPath.getConnection().getTabular()
          .getMemoryConnection().getDataPath(dataPath.getLogicalName())
          .getOrCreateRelationDef()
          .mergeStruct(this.runtimeRelationDef)
          .getDataPath()
          .getInsertStream();
      }

      /**
       * Printing the table header (not the column header)
       * (ie name and comment)
       */
      if (getPrintTableHeader(actualFormat, seenDataPath)) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!(dataPath instanceof MemoryDataPath)) {
          // name is not random
          stringBuilder.append(dataPath.getName());
        }
        if (dataPath.getComment() != null) {
          if (stringBuilder.length() != 0) {
            stringBuilder.append(": ");
          }
          // trim to get a consistent output if the dev delete it
          stringBuilder.append(dataPath.getComment().trim());
        }
        System.out.println(stringBuilder);
      }

      while (dataPathSelectStream.next()) {

        sampleCounter++;
        if (sampleCounter > maxSampleSize) {
          break;
        }

        List<?> objects = dataPathSelectStream.getObjects();
        if (this.executableInsertStream != null) {
          this.executableInsertStream.insert(objects);
        }

        /**
         * Check that the number of values is the same as the number of columns
         */
        if (objects.size() != runtimeRelationDef.getColumnsSize()) {
          throw new IllegalStateException("The data path (" + runtimeRelationDef.getDataPath() + ", Logical Name: " + runtimeRelationDef.getDataPath().getLogicalName() + ") has " + runtimeRelationDef.getColumnsSize() + " columns (" + runtimeRelationDef.getColumnDefs().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")) + ") but has (" + objects.size() + ") values (" + objects + ") to print. This is inconsistent.");
        }

        /**
         * Max length calculation of each column
         */
        for (ColumnDef<?> columnDef : runtimeRelationDef.getColumnDefs()) {
          if (this.printBuilder.colorsColumnName != null) {
            if (columnDef.getColumnNameNormalized().equals(this.printBuilder.colorsColumnName)) {
              /**
               * we take the color column from the runtime and not from the original resource.
               * Why? because a <select> of a table is another resource (ie it's a SQLRequest)
               * Therefore the column of the table is not the same as the column of the executable
               */
              this.colorsColumnDef = columnDef;
              continue;
            }
          }
          Integer i = columnDef.getColumnPosition();
          Object value = dataPathSelectStream.getObject(i);
          String string = this.getPrintedString(value, columnDef);
          // Add
          int length = string.length();
          Integer actualLength = columnPositionLengthMap.get(i);
          if (actualLength == null) {
            columnPositionLengthMap.put(i, length);
            continue;
          }
          if (actualLength < length) {
            columnPositionLengthMap.put(i, length);
          }
        }
      }
    }

    /**
     * Add the column name size in the max length calculation
     * (ie Be sure to not cut a column header if there is no data)
     */
    for (int i = 1; i <= runtimeRelationDef.getColumnsSize(); i++) {
      int length = runtimeRelationDef.getColumnDef(i).getColumnName().length();
      Integer max = columnPositionLengthMap.get(i);
      if (max == null) {
        columnPositionLengthMap.put(i, length);
        continue;
      }
      if (max < length) {
        columnPositionLengthMap.put(i, length);
      }
    }

    /**
     * Be sure to pick the max of all seen line when printing in streaming mode
     */
    if (actualFormat == STREAM) {
      /**
       * Here previousColumnPositionLengthMap is a reference, meaning
       * than when we update it, we get the change the next time
       */
      Map<Integer, Integer> previousColumnPositionLengthMap = printer.seenDataPathColumnLengths.get(dataPath.getLogicalName());
      if (previousColumnPositionLengthMap != null) {
        for (int i : previousColumnPositionLengthMap.keySet()) {
          Integer previousSize = previousColumnPositionLengthMap.get(i);
          Integer actualSize = columnPositionLengthMap.get(i);
          if (actualSize > previousSize) {
            if (!columnPositionLengthMapChanged) {
              columnPositionLengthMapChanged = true;
            }
            previousColumnPositionLengthMap.put(i, actualSize);
          }
        }
        return previousColumnPositionLengthMap;
      }
      printer.seenDataPathColumnLengths.put(dataPath.getLogicalName(), columnPositionLengthMap);
    }

    return columnPositionLengthMap;

  }

  /**
   * @return the select stream
   */
  private SelectStream getSelectStream(DataPath dataPath) {
    if (dataPath.isRuntime()) {
      return dataPath.execute().getSelectStreamSafe();
    }
    return dataPath.getSelectStreamSafe();
  }


  /**
   * Return a format string for a column
   */
  private String getColumnFormatString(ColumnDef<?> columnDef) {
    StringBuilder formatString = new StringBuilder();
    formatString.append("%"); // Template start

    boolean isRightJustified = columnDef.getDataType().isNumber();

    Integer i = columnDef.getColumnPosition();
    boolean notLastColumn = i != this.runtimeRelationDef.getColumnsSize();
    if (!isRightJustified) {
      // Left Justified
      // If this is the last column, we don't need to have tail spaces (ie spaces at the end)
      // Some ide will delete them and that's bad storage
      if (notLastColumn) {
        formatString
          .append("-") // result left-justified. (Default is right)
          .append(columnPositionLengthMap.get(i)); // the width
      }
    } else {
      // Right Justified
      formatString
        .append(columnPositionLengthMap.get(i)); // the width
    }
    // String (s is a placeholder for string)
    formatString
      .append("s");
    // Add column space if this is not the last column
    if (notLastColumn) {
      formatString.append(new String(new char[this.printBuilder.horizontalColumnMarginCount]).replace("\0", " ")); // The spaces between column
    }
    return formatString.toString();
  }


  /**
   * A format string without color
   * Building the format string
   * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html">...</a>
   */
  private String getHeaderFormatString() {

    StringBuilder formatString = new StringBuilder();
    for (int i = 1; i <= this.runtimeRelationDef.getColumnsSize(); i++) {
      ColumnDef<?> columnDef = this.runtimeRelationDef.getColumnDef(i);
      if (columnDef.equals(this.colorsColumnDef)) {
        continue;
      }
      formatString.append(this.getColumnFormatString(columnDef));
    }
    return formatString.toString();
  }

  private int getFooterSeparationLineCount() {
    if (actualFormat == STREAM) {
      return 0;
    }
    if (actualFormat == PIPE) {
      return 0;
    }
    return this.printBuilder.footerSeparationLineCount;
  }

  private boolean getPrintColumnHeaders() {
    if (actualFormat == STREAM) {
      /**
       * We print the headers each time that the columns length has changed
       */
      if (columnPositionLengthMapChanged) {
        columnPositionLengthMapChanged = false;
        return true;
      }
      return !seenDataPath;
    }
    if (actualFormat == PIPE) {
      return false;
    }
    return this.printBuilder.printColumnHeaders;
  }

  public void print() {


    // Adding headers
    String formatString = this.getHeaderFormatString();
    if (getPrintColumnHeaders()) {

      Object[] headerColumns = runtimeRelationDef.getColumnDefs()
        .stream()
        .filter(columnDef -> !columnDef.equals(colorsColumnDef))
        .map(ColumnDef::getColumnName)
        .toArray();
      String header = String.format(formatString,
        headerColumns);
      System.out.println(header);

      /**
       * Separation Line
       */
      StringBuilder line = new StringBuilder();
      List<Integer> columnLengthsWithoutHighlightColumn = columnPositionLengthMap
        .entrySet()
        .stream()
        .filter(colPositionMaxLengthEntry -> {
          if (colorsColumnDef == null) {
            return true;
          }
          // filter outer the highlight column
          return !colPositionMaxLengthEntry.getKey().equals(colorsColumnDef.getColumnPosition());
        })
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
      for (Integer max : columnLengthsWithoutHighlightColumn) {
        line
          .append(new String(new char[max]).replace("\0", "-"))
          .append(new String(new char[this.printBuilder.horizontalColumnMarginCount]).replace("\0", " "));
      }
      // trim to delete the last spaces so that no diff will be seen in the IDE
      // no bad storage
      // IDE may delete the trailing space before commit if we don't we will see a diff each time
      String lineString = line.toString().trim();
      System.out.println(lineString);

    }

    /**
     * Print the data
     */
    long recordCounter = 0;
    SelectStream selectStream;
    if (executableInsertStream != null) {
      executableInsertStream.close();
      selectStream = executableInsertStream.getDataPath().getSelectStreamSafe();
    } else {
      selectStream = dataPath.getSelectStreamSafe();
    }
    try (SelectStream dataPathSelectStream = selectStream) {
      while (dataPathSelectStream.next()) {
        recordCounter++;
        List<Object> record = new ArrayList<>();
        StringBuilder formatStringColumns = new StringBuilder();
        Map<Integer, PrinterColor> columnPositionColorMap = new HashMap<>();
        RelationDef runtimeRelationDef = dataPathSelectStream.getRuntimeRelationDef();
        if (colorsColumnDef != null) {
          String colorsDef = dataPathSelectStream.getString(colorsColumnDef.getColumnPosition());
          try {
            switch (colorsDef.length()) {
              case 0:
                break;
              case 1:
                /**
                 * One color for the whole record
                 */
                PrinterColor color = PrinterColor.cast(colorsDef);
                columnPositionColorMap = runtimeRelationDef
                  .getColumnDefs()
                  .stream()
                  .collect(Collectors.toMap(
                    ColumnDef::getColumnPosition,
                    c -> color
                  ));
                break;
              default:
                columnPositionColorMap = this.getPositionColorMap(colorsDef);
                break;
            }
          } catch (CastException e) {
            throw new IllegalStateException("The color value (" + colorsDef + ") in the column (" + colorsColumnDef + ") at record " + recordCounter + " is not valid. Error: " + e.getMessage(), e);
          }

        }
        for (ColumnDef columnDef : runtimeRelationDef.getColumnDefs()) {
          Integer i = columnDef.getColumnPosition();
          Object value = dataPathSelectStream.getObject(i);
          if (columnDef.equals(colorsColumnDef)) {
            continue;
          }
          String string = this.getPrintedString(value, columnDef);
          PrinterColor printerColor = columnPositionColorMap.get(columnDef.getColumnPosition());
          formatStringColumns.append(PrinterColor.addColorIfNotNull(this.getColumnFormatString(columnDef), printerColor));
          record.add(string);
        }
        System.out.printf(
          (formatStringColumns) + "%n",
          // Null show up as null, we want it to be the empty string
          // to show that there is nothing
          // It comes from the fact that size is not always implemented and may return null
          record.stream()
            .map(o -> Objects.requireNonNullElse(o, "")).toArray());
      }
    }


    /**
     * A separation between 2 print
     */
    for (int i = 0; i < getFooterSeparationLineCount(); i++) {
      System.out.println();
    }
  }

  private Map<Integer, PrinterColor> getPositionColorMap(String colorSpec) throws CastException {

    if (colorSpec == null || colorSpec.trim().isEmpty()) {
      return new HashMap<>();
    }

    // whole record highlighting
    if (colorSpec.length() == 1) {
      return new HashMap<>();
    }

    Map<Integer, PrinterColor> result = new HashMap<>();

    String[] pairs = colorSpec.trim().split(",");

    for (String pair : pairs) {
      if (pair.isEmpty()) continue;

      StringBuilder numberPart = new StringBuilder();
      StringBuilder colorPart = new StringBuilder();
      boolean numberPartClose = false;
      for (int i = 0; i < pair.length(); i++) {
        char c = pair.charAt(i);
        if (numberPartClose) {
          colorPart.append(c);
        }
        if (Character.isDigit(c)) {
          numberPart.append(c);
          continue;
        }
        numberPartClose = true;
        colorPart.append(c);
      }

      Integer columnPosition;
      try {
        columnPosition = Casts.cast(numberPart.toString(), Integer.class);
      } catch (CastException e) {
        throw new CastException("Invalid number highlight: " + numberPart + " is not an integer in " + pair, e);
      }

      // Parse the color part
      PrinterColor printerColor;
      try {
        printerColor = PrinterColor.cast(colorPart.toString());
      } catch (CastException e) {
        throw new CastException("Invalid color (" + colorPart + ") in pair: " + numberPart + ". Error: " + e.getMessage(), e);
      }

      result.put(columnPosition, printerColor);
    }

    return result;

  }
}
