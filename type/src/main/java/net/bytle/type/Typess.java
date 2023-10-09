package net.bytle.type;

import java.math.BigInteger;
import java.util.Date;

public class Typess {


  /**
   * The number of element for data type
   * By default, {@link Long#MAX_VALUE}
   * @param clazz
   * @return
   */
  public static  Long getMaxByClass(Class clazz) {

    /**
     * For the date, the {@link Date constructor} is based on a long
     * By default, no date will go so far,
     * this is then a good approximation
     */
    long maxSize = Long.MAX_VALUE;
    if (clazz.equals(BigInteger.class)){
      /**
       * From the doc
       */
      maxSize = (long) Math.pow(2, Integer.MAX_VALUE);
    } else if (clazz.equals(Integer.class)){
      maxSize = (long) Integer.MAX_VALUE;
    } else if (clazz.equals(Short.class)){
      maxSize = (long) Short.MAX_VALUE;
    }
    return maxSize;
  }
}
