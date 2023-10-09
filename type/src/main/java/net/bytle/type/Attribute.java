package net.bytle.type;

/**
 * An interface for an attribute that is used normally against an enum
 * <p>
 * <p>
 * attribute and not property because the product is called `tabulify`
 */
public interface Attribute {


  /**
   * They key is the to string function
   *
   * This key is normalized {@link Key#toNormalizedKey(String)} (that is not uppercase, minus or underscore and trim dependent)
   * to:
   * - determine uniqueness
   * - cast to an enum (ie {@link Casts#cast(Object, Class)}})
   *
   * The key published to the outside world is done with the {@link Key#toCamelCaseValue(String)}
   * Public key are key that are going into external artifacts
   * such as configuration file, console output or workflow file
   *
   */


  /**
   * @return the description of the attribute
   */
  String getDescription();

  /**
   * Optional the class of the value
   * It may be a java class or an enum class
   * (that could/should implement {@link AttributeValue} to define a domain)
   * It's used to validate the value when a {@link Variable} is created
   */
  Class<?> getValueClazz();


  /**
   *
   * @return a fix default value
   */
  Object getDefaultValue();


}
