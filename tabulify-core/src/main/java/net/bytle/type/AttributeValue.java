package net.bytle.type;

/**
 * An interface that represents an enum value (not a key)
 * so that we have a description to show to the user
 * and the value can be cast
 */
public interface AttributeValue {


  /**
   * @return The description of the value
   */
  String getDescription();


}
