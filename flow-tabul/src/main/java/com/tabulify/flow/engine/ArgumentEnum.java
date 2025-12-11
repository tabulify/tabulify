package com.tabulify.flow.engine;


import com.tabulify.conf.AttributeEnum;

/**
 * An attribute enum
 */
public interface ArgumentEnum extends AttributeEnum {


  /**
   * @return the description of the attribute
   */
  String getDescription();

  /**
   * @return a fix default value
   * Used when listing attribute
   */
  Object getDefaultValue();

  /**
   * The target class of the value
   */
  Class<?> getValueClazz();

  default boolean getMandatory() {
    return false;
  }

}
