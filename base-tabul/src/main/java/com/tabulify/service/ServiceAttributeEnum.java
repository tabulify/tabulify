package com.tabulify.service;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.AttributeEnumParameter;

/**
 * An interface to no mix common {@link AttributeEnum}
 * with service attribute. They should be enum and not created dynamically
 * but with {@link com.tabulify.type.Casts#cast(Object, Class)} from the enum class
 */
public interface ServiceAttributeEnum extends AttributeEnumParameter {


  /**
   * If the value is a list or a map,
   * the class of the element
   */
  Class<?> getCollectionElementClazz();

  /**
   * If the value is a map,
   * the class of the value
   */
  Class<?> getCollectionValueClazz();

}
