package com.tabulify.conf;

import net.bytle.exception.CastException;
import net.bytle.exception.NoValueException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.Strings;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A super set of a key pair value
 * adding functionality such:
 * * as conf/secret via {@link #setRawValue(String)} and {@link #setPlainValue(Object)} (Object)}
 * * key case independence (via {@link AttributeEnum} that uses a {@link KeyNormalizer})
 */
public class Attribute implements Comparable<Attribute> {


  private AttributeEnum attributeEnum;

  /**
   * Origin of the value
   */
  private final Origin origin;

  /**
   * The value as found in the text file / operating system
   * so that we can:
   * * recreate the text file
   * * send this data to the log / console output (only if it's really needed)
   * This value is returned as an empty string by {@link #getRawValue()} if it comes from Os and {@link #isOsSecret} is true
   */
  private String rawValue;

  /**
   * A plain value (decrypted if needed and casted as {@link AttributeEnum#getValueClazz()})
   */
  private Object plainValue;


  /**
   * A function that gives the value
   */
  private Supplier<?> valueProvider;

  private boolean isOsSecret = false;

  private Attribute(AttributeEnum attributeEnum, Origin origin) {

    this.attributeEnum = attributeEnum;
    if (origin == null) {
      throw new IllegalArgumentException("The origin of the variable (" + this + ") was null, it should not");
    }
    // origin is important for security reason, that's why it is in the constructor
    this.origin = origin;

    if (this.origin == Origin.OS) {
      String normalizedAttName = this.attributeEnum.toString().toLowerCase();
      for (String secWord : Arrays.asList("secret", "key", "pwd", "password", "passphrase")) {
        if (normalizedAttName.contains(secWord)) {
          isOsSecret = true;
          break;
        }
      }
    }

  }

  public static Attribute create(String name, Origin origin) {
    return createWithClass(name, origin, String.class);
  }


  /**
   * Utility class of {@link #createWithClassAndDefault(String, Origin, Class, Object)}
   * where the default value is null
   */
  public static Attribute createWithClass(String name, Origin origin, Class<?> clazz) {
    return createWithClassAndDefault(name, origin, clazz, null);
  }

  /**
   * Variable may be instantiated at runtime without knowing the name at compile time.
   * For instance,
   * * the properties of a jdbc driver. We don't master that
   * * dynamic variable such as the backref reference of a regexp $1, ...
   * So we need to be able to create a variable by name
   */
  public static Attribute createWithClassAndDefault(String name, Origin origin, Class<?> clazz, Object defaultValue) {


    AttributeEnum attributeFromName = new AttributeEnum() {


      @Override
      public String getDescription() {
        return name;
      }

      @Override
      public Class<?> getValueClazz() {
        return clazz;
      }

      @Override
      public Object getDefaultValue() {
        return defaultValue;
      }

      /**
       * @return the unique string identifier (mandatory)
       */
      @Override
      public String toString() {
        return name;
      }

    };

    return new Attribute(attributeFromName, origin);
  }

  public static Attribute create(AttributeEnum attribute, Origin origin) {

    return new Attribute(attribute, origin);
  }


  public Origin getOrigin() {
    return this.origin;
  }


  public String getRawValue() {
    if (isOsSecret) {
      return "";
    }
    return this.rawValue;
  }

  /**
   * @return the value to be used in the application in clear
   */
  public Object getValueOrDefault() throws NoValueException {
    try {

      return this.getValue();

    } catch (NoValueException e) {

      Object value = this.attributeEnum.getDefaultValue();
      if (value != null) {
        return value;
      }

      throw new NoValueException("No value or default value found");
    }

  }


  @SuppressWarnings("unused")
  public Attribute setPlainValue(Object value) {
    Class<?> valueClazz = this.attributeEnum.getValueClazz();
    if (valueClazz == null) {
      throw new ClassCastException("The class of the attribute " + this.attributeEnum + " should not be null");
    }
    try {
      this.plainValue = Casts.cast(value, valueClazz);
    } catch (CastException e) {
      // It's not a secret as it's the original value
      throw new ClassCastException("The value " + value + " of " + this.getAttributeMetadata() + " cannot be converted to a " + valueClazz);
    }
    return this;
  }

  @Override
  public String toString() {
    /**
     * No clear value in the log
     */
    return this.attributeEnum.toString() + " = " + Strings.createFromObjectNullSafe(this.rawValue);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return this.attributeEnum.equals(((Attribute) o).attributeEnum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.attributeEnum.hashCode());
  }

  public AttributeEnum getAttributeMetadata() {
    return this.attributeEnum;
  }

  /**
   * Utility wrapper around {@link Casts#cast(Object, Class)}
   */
  @SuppressWarnings("unused")
  public <T> T getValueOrDefaultCastAs(Class<T> clazz) throws NoValueException, CastException {
    Object object = this.getValueOrDefault();
    return Casts.cast(object, clazz);
  }


  public Object getValue() throws NoValueException {
    if (this.plainValue != null) {
      return this.plainValue;
    }
    if (this.valueProvider != null) {
      return this.valueProvider.get();
    }
    throw new NoValueException("No value found");
  }

  @SuppressWarnings("unused")
  public boolean hasNullValue() {
    try {
      this.getValueOrDefault();
      return true;
    } catch (NoValueException e) {
      return false;
    }
  }

  @SuppressWarnings("unused")
  public Object getValueOrDefaultOrNull() {
    try {
      return this.getValueOrDefault();
    } catch (NoValueException e) {
      return null;
    }
  }

  public Object getValueOrNull() {
    try {
      return this.getValue();
    } catch (NoValueException e) {
      return null;
    }
  }


  /**
   * @return the string value or the empty string if not found
   */
  public String getValueOrDefaultAsStringNotNull() {
    try {
      return String.valueOf(getValueOrDefault());
    } catch (NoValueException e) {
      return "";
    }
  }

  /**
   * @param valueProvider - the function that should return the value (use it if you want to get the value at runtime
   *                      such as with external vault)
   * @return the variable
   */
  public Attribute setValueProvider(Supplier<?> valueProvider) {
    this.valueProvider = valueProvider;
    return this;
  }


  public void setAttributeMetadata(AttributeEnum attributeEnum) {
    this.attributeEnum = attributeEnum;
  }


  @Override
  public int compareTo(Attribute o) {
    return this.attributeEnum.toString().compareTo(o.attributeEnum.toString());
  }

  public Attribute setRawValue(String string) {
    this.rawValue = string;
    return this;
  }

}
