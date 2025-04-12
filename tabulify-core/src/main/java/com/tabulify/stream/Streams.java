package com.tabulify.stream;

import com.tabulify.fs.FsDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import net.bytle.log.Log;
import net.bytle.log.Logs;
import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Streams {

  private static final Log LOGGER = Logs.createFromClazz(Streams.class);


  /**
   * Print the table outpustream
   * align to the left
   *
   * @param selectStream - the select stream
   */
  public static void print(SelectStream selectStream) {

    assert selectStream != null : "Select Stream is null";

    /**
     * Scanning the values:
     *   * to determine the max length
     *   * to verify that the number of values is the same than the number of columns
     */
    List<List<?>> values = new ArrayList<>();
    Map<Integer, Integer> maxByColumnPosition = new HashMap<>();
    final RelationDef relationDef = selectStream.getRuntimeRelationDef();
    while (selectStream.next()) {
      List<?> objects = selectStream.getObjects();

      /**
       * Check that the number of values is the same as the number of columns
       */
      if (objects.size() != relationDef.getColumnsSize()) {
        throw new IllegalStateException("The data path (" + relationDef.getDataPath() + ", Logical Name: " + relationDef.getDataPath().getLogicalName() + ") has " + relationDef.getColumnsSize() + " columns (" + relationDef.getColumnDefs().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")) + ") but has (" + objects.size() + ") values (" + objects + ") to print. This is inconsistent.");
      }


      /**
       * Max length calculation of each columns
       */
      List<String> stringObjects = new ArrayList<>();
      for (int i = 1; i <= relationDef.getColumnsSize(); i++) {
        String string = selectStream.getString(i);
        if (string == null) {
          string = "";
        }
        // make the non-visible character visible
        string = Strings.createFromString(string)
            .toPrintableCharacter();
        stringObjects.add(string);
        int length = string.length();
        Integer max = maxByColumnPosition.get(i);
        if (max == null) {
          maxByColumnPosition.put(i, length);
          continue;
        }
        if (max < length) {
          maxByColumnPosition.put(i, length);
        }
      }
      values.add(stringObjects);
    }

    /**
     * Be sure to not cut a column
     * if there is no data
     */
    for (int i = 1; i <= relationDef.getColumnsSize(); i++) {
      int length = relationDef.getColumnDef(i).getColumnName().length();
      Integer max = maxByColumnPosition.get(i);
      if (max == null) {
        maxByColumnPosition.put(i, length);
        continue;
      }
      if (max < length) {
        maxByColumnPosition.put(i, length);
      }
    }

    // Number of space between columns
    int spacesBetweenCols = 3;

    /**
     * Building the format string
     * https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html
     */
    StringBuilder formatString = new StringBuilder();
    for (int i = 1; i <= relationDef.getColumnsSize(); i++) {
      formatString.append("%"); // Template start
      if (!relationDef.getColumnDef(i).getDataType().isNumeric()) {
        formatString.append("-"); // result left-justified. (Default is right)
      }
      formatString
        .append(maxByColumnPosition.get(i)) // the width
        .append("s") // S is a placeholder for string
        .append(new String(new char[spacesBetweenCols]).replace("\0", " ")); // The spaces between column
    }
    LOGGER.fine("The format string is (" + formatString + ")");

    /**
     * Print the header if it's not a header text file
     */
    if (!(relationDef.getColumnDefs().size() == 1 && selectStream.getDataPath() instanceof FsDataPath)) {

      String header = String.format(formatString.toString(),
        relationDef.getColumnDefs()
          .stream()
          .map(ColumnDef::getColumnName)
          .toArray());
      System.out.println(header);

      /**
       * Separation Line
       */
      StringBuilder line = new StringBuilder();
      for (Integer max : maxByColumnPosition.values()) {
        line
          .append(new String(new char[max]).replace("\0", "-"))
          .append(new String(new char[spacesBetweenCols]).replace("\0", " "));
      }
      System.out.println(line);

    }


    /**
     * Print the data
     */
    for (List<?> row : values) {
      System.out.printf(
        (formatString) + "%n",
        // Null show up as null, we want it to be the empty string
        // to show that there is nothing
        // It comes from the fact that size is not always implemented and may returned null
        row.stream()
          .map(o -> {
            if (o == null) {
              return "";
            } else {
              return o;
            }
          }).toArray());
    }

  }




}
