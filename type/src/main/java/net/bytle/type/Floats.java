package net.bytle.type;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Long static function
 */
public class Floats {

  /**
   * Format for parsing long
   */
  private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ROOT);

  Float fromString(String s){
    try {
      return numberFormat.parse(s).floatValue();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

}
