package com.tabulify.text.yaml;

import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.MapKeyIndependent;

import java.util.List;
import java.util.Map;

public class YamlCast {

  public static <K, V> Map<K, V> castToSameMap(Object object, Class<K> keyClass, Class<V> valueClass) throws CastException {

    if (object instanceof Map) {
      return Casts.castToSameMap(object, keyClass, valueClass);
    }
    String message = "The values (" + object + ") are not a map";
    if (object instanceof List) {
      message += " but a list. You should delete the minus characters before the keys.";
    } else {
      message += " but a " + object.getClass().getSimpleName();
    }
    throw new CastException(message);

  }

  public static <V> MapKeyIndependent<V> castToMapKeyIndependent(Object dataResourceObject, Class<V> classValue) {
    try {
      return MapKeyIndependent.createFrom(
        YamlCast.castToSameMap(dataResourceObject, String.class, classValue),
        classValue
      );
    } catch (CastException e) {
      throw new InternalException("Should not throw as every object as a string representation", e);
    }
  }
}
