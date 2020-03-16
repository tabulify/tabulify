package net.bytle.type;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class Integers {

  /**
   * Format for parsing integers. Not thread-safe
   */
  private final NumberFormat integerFormat = NumberFormat.getIntegerInstance(Locale.ROOT);

  /**
   *
   * @param s
   * @return an integer from the string s or a runtime exception
   */
  Integer valueOf(String s){
    try {
      return integerFormat.parse(s).intValue();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static Double toDouble(Object o) {
    if (o instanceof Integer) {
      o = ((Integer) o).doubleValue();
    }

    return (Double) o;
  }

  public static Integer toInteger(Object o) {
    if (o instanceof Double) {
      o = ((Double) o).intValue();
    }
    return (Integer) o;
  }
}
