package net.bytle.type;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Long static function
 */
public class Longs {

  /**
   * Format for parsing long
   */
  private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ROOT);

  Long valueOf(String s){
    try {
      return numberFormat.parse(s).longValue();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
