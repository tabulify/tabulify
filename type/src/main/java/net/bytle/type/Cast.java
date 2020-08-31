package net.bytle.type;

import java.math.BigInteger;

public class Cast {


  public static Object cast(Object sourceObject, Class<?> targetClass) {

    if (sourceObject.getClass()==String.class){
      if (targetClass== BigInteger.class){
        sourceObject = new BigInteger((String) sourceObject);
      }
    }
    return sourceObject;
  }

}
