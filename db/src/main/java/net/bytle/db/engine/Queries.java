package net.bytle.db.engine;

import net.bytle.fs.Fs;
import net.bytle.log.Log;
import net.bytle.db.DbLoggers;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Queries {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

  private static final String SELECT_WORD = "SELECT";
  private static final String WITH_WORD = "WITH";

  private static final List<String> queryFirstWords = Arrays.asList(new String[]{SELECT_WORD, WITH_WORD});

  /**
   * @param s
   * @return true if a the string is a query (ie start with the 'select' word
   */
  public static Boolean isQuery(String s) {

    if (s == null) {
      return false;

    }
    s = s.trim();
    List<String> seps = Arrays.asList(" ", "\r\n", "\n", "\t");
    Integer sepIndexMin = null; // Not found
    for (String sep : seps) {
      int sepIndex = s.indexOf(sep);
      if (sepIndex != -1 && (sepIndexMin == null || sepIndex < sepIndexMin)) {
        sepIndexMin = sepIndex;
      }
    }
    if (sepIndexMin == null) {
      return false;
    }

    s = s.substring(0, sepIndexMin).toUpperCase();


    return queryFirstWords.contains(s);

  }


  /**
   *
   * @param path
   * @return the first query of a file
   */
  public static String getQuery(Path path) {

    String query = Fs.getFileContent(path);
    if (Queries.isQuery(query)) {
      return query;
    } else {
      return null;
    }

  }
}
