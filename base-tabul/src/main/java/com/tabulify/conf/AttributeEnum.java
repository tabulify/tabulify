package com.tabulify.conf;


import com.tabulify.type.KeyInterface;
import com.tabulify.type.KeyNormalizer;

/**
 * An attribute is a name for a {@link Attribute variable} with extra properties.
 * <p></p>
 * An interface for an attribute that is used against an enum
 * so that we get:
 * * a description
 * * a default value
 * Class is an attribute of the value of the variable.
 * <p>
 * <p>
 * Attribute and not property because the product is called `tabulify`
 * <p></p>
 * Advantage over Abstract Class
 * * easy casting
 * * easy switch statement (switch require an enum, object are not allowed)
 * * no need to create a list of all attributes (builtin)
 * Disadvantage over Abstract Class
 * * No inheritance (adding a function in the interface will break all attribute)
 * * No equality for attribute created on the fly (not used though)
 * * No simple get identifier (toString should be implemented right)
 */
public interface AttributeEnum extends KeyInterface {


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
   * It's used to create relational column
   * For complex value such as map and list, you need to take over
   */
  Class<?> getValueClazz();

  /**
   * @return the enum in a normalized way
   */
  default KeyNormalizer getKeyNormalized() {
    return KeyNormalizer.createSafe(this.name());
  }

  /**
   * @return if the attribute can be set
   */
  default boolean getIsUpdatable() {
    return true;
  }

}
