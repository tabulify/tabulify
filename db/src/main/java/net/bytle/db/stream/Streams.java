package net.bytle.db.stream;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.SqlDataTypesManager;
import net.bytle.db.model.RelationDef;
import net.bytle.log.Log;

import java.util.*;

public class Streams {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


  /**
   * Print the table outpustream
   * align to the left
   *
   * @param selectStream
   */
  public static void print(SelectStream selectStream) {

    assert selectStream != null : "Select Stream is null";

    List<List<Object>> values = new ArrayList<>();
    Map<Integer, Integer> maxs = new HashMap<>();
    final RelationDef tableDef = selectStream.getSelectDataDef();
    while (selectStream.next()) {
      values.add(selectStream.getObjects());
      for (int i = 0; i < tableDef.getColumnsSize(); i++) {
        String string = selectStream.getString(i);
        if (string == null) {
          string = "";
        }
        int length = string.length();
        Integer max = maxs.get(i);
        if (max == null) {
          maxs.put(i, length);
          continue;
        }
        if (max < length) {
          maxs.put(i, length);
        }
      }
    }
    for (int i = 0; i < tableDef.getColumnsSize(); i++) {
      int length = tableDef.getColumnDef(i).getColumnName().length();
      Integer max = maxs.get(i);
      if (max == null) {
        maxs.put(i, length);
        continue;
      }
      if (max < length) {
        maxs.put(i, length);
      }
    }

    // Number of space between columns
    int spacesBetweenCols = 3;

    // Building the format string
    // https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html
    StringBuilder formatString = new StringBuilder();
    for (int i = 0; i < tableDef.getColumnsSize(); i++) {
      formatString.append("%"); // Template start
      if (!SqlDataTypesManager.isNumeric(tableDef.getColumnDef(i).getDataType().getTypeCode())) {
        formatString.append("-"); // result left-justified. (Default is right)
      }
      formatString
        .append(maxs.get(i)) // the width
        .append("s") // S is a placeholder for string
        .append(new String(new char[spacesBetweenCols]).replace("\0", " ")); // The spaces between column
    }
    LOGGER.fine("The format string is (" + formatString + ")");

    // Print the header
    System.out.println(String.format(formatString.toString(),
      Arrays.stream(tableDef.getColumnDefs())
        .map(s -> s.getColumnName())
        .toArray()));

    // Separation Line
    StringBuilder line = new StringBuilder();
    for (Integer max : maxs.values()) {
      line
        .append(new String(new char[max]).replace("\0", "-"))
        .append(new String(new char[spacesBetweenCols]).replace("\0", " "));
    }
    System.out.println(line);

    // Print the data
    for (List<Object> row : values) {
      System.out.println(String.format(formatString.toString(), row.toArray()));
    }

  }


}
