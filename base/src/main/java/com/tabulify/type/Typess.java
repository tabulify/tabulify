package com.tabulify.type;

import java.math.BigInteger;
import java.util.Date;

public class Typess {


  /**
   * The number of element for a data type
   * By default, {@link Long#MAX_VALUE}
   */
  @SuppressWarnings("unused")
  public static  Long getMaxByClass(Class<?> clazz) {


    if (clazz.equals(BigInteger.class)){
      /**
       * From the doc
       */
      return (long) Math.pow(2, Integer.MAX_VALUE);
    }
    if (clazz.equals(Integer.class)){
      return (long) Integer.MAX_VALUE;
    }
    if (clazz.equals(Short.class)){
     return (long) Short.MAX_VALUE;
    }
    /**
     * For the date, the {@link Date constructor} is based on a long
     * By default, no date will go so far,
     * this is then a good approximation
     */
    return Long.MAX_VALUE;

  }
}
