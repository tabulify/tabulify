package com.tabulify.conf;

import com.tabulify.stream.SelectStream;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.exception.CastException;
import net.bytle.exception.ExceptionWrapper;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A super set of a key pair value
 * adding functionality such:
 * * as conf/secret via {@link #setRawValue(Object)} and {@link #setPlainValue(Object)} (Object)}
 * * key case independence (via {@link AttributeEnum} that uses a {@link KeyNormalizer})
 * Attribute key may be created dynamically (for instance from a record {@link SelectStream#getAttributes()}
 * Templating function over the value is within the vault {@link com.tabulify.Vault} that holds the env
 */
public class Attribute implements Comparable<Attribute> {


  public static final String NOT_SHOWN_SECRET = "xxxxxx";
  /**
   * password and passphrase are widely sensitive
   * We set them by default as secret
   */
  private final boolean isTabulifySecretAttribute;

  private AttributeEnum attributeEnum;

  /**
   * Origin of the value
   */
  private final Origin origin;

  /**
   * The value as:
   * * found in the text file / operating system
   * * as set by java method (set)
   * so that we can:
   * * recreate the text file
   * * send this data to the log / console output (only if it's really needed)
   * This value is returned as an empty string by {@link #getRawValue()} if it comes from Os and {@link #isOsSecret} is true
   * It's an object because an attribute may be also a map or a list
   */
  private Object rawValue;

  /**
   * A plain value (decrypted if needed, and casted as {@link AttributeEnum#getValueClazz()})
   */
  private Object plainValue;


  /**
   * A function that gives the value
   */
  private Supplier<?> valueProvider;

  private final boolean isOsSecret;

  private Attribute(AttributeEnum attributeEnum, Origin origin) {

    this.attributeEnum = attributeEnum;
    if (origin == null) {
      throw new IllegalArgumentException("The origin of the variable (" + this + ") was null, it should not");
    }
    // origin is important for security reason, that's why it is in the constructor
    this.origin = origin;

    boolean isOsSecretTemp = false;
    if (this.origin == Origin.OS) {
      String normalizedAttName = this.attributeEnum.toString().toLowerCase();
      for (String secWord : Arrays.asList("secret", "key", "pwd", "password", "passphrase")) {
        if (normalizedAttName.contains(secWord)) {
          isOsSecretTemp = true;
          break;
        }
      }
    }
    isOsSecret = isOsSecretTemp;

    String normalizedAttName = this.attributeEnum.toString().toLowerCase();
    boolean isTabulifySecretAttributeTemp = false;
    for (String secWord : Arrays.asList("password", "passphrase")) {
      if (normalizedAttName.contains(secWord)) {
        isTabulifySecretAttributeTemp = true;
        break;
      }
    }
    isTabulifySecretAttribute = isTabulifySecretAttributeTemp;


  }

  public static Attribute create(String name, Origin origin) {
    return createWithClass(name, origin, String.class);
  }


  /**
   * Utility class of {@link #createWithClassAndDefault(KeyNormalizer, Origin, Class, Object)}
   * where the default value is null
   */
  public static Attribute createWithClass(String name, Origin origin, Class<?> clazz) {
    return createWithClassAndDefault(KeyNormalizer.createSafe(name), origin, clazz, null);
  }

  /**
   * Variable may be instantiated at runtime without knowing the name at compile time.
   * For instance,
   * * the properties of a jdbc driver. We don't master that
   * * dynamic variable such as the backref reference of a regexp $1, ...
   * So we need to be able to create a variable by name
   */
  public static Attribute createWithClassAndDefault(KeyNormalizer name, Origin origin, Class<?> clazz, Object defaultValue) {


    AttributeEnum attributeFromName = new AttributeEnum() {


      @Override
      public String name() {
        return name.toString();
      }

      @Override
      public KeyNormalizer toKeyNormalizer() {
        return name;
      }

      @Override
      public String getDescription() {
        return name.toString();
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
        return name.toString();
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


  /**
   * Raw value is used to recreate the configuration vault
   * and in the log.
   * We can't return a fake value if this is a secret.
   */
  public Object getRawValue() {
    if (isOsSecret) {
      return NOT_SHOWN_SECRET;
    }
    return this.rawValue;
  }

  /**
   * @return the value to be used in the application in clear
   */
  public Object getValueOrDefault() {
    try {

      return this.getValue();

    } catch (NoValueException e) {

      return this.attributeEnum.getDefaultValue();

    }

  }


  /**
   * @param value - the value set at runtime or the decoded value of raw values
   */
  public Attribute setPlainValue(Object value) {
    Class<?> valueClazz = this.attributeEnum.getValueClazz();
    if (valueClazz == null) {
      throw new ClassCastException("The class of the attribute " + this.attributeEnum + " should not be null");
    }

    this.plainValue = castToPlainValue(value, valueClazz);

    if (this.rawValue == null) {
      setRawValue(plainValue);
    }
    return this;
  }

  /**
   * Cast to a plain value
   */
  private Object castToPlainValue(Object value, Class<?> valueClazz) {

    try {

      // The only tabulify specific type for now
      if (valueClazz == DataUriStringNode.class) {
        return DataUriStringNode.createFromString(value.toString());
      }

      // All other
      return Casts.cast(value, valueClazz);

    } catch (CastException e) {
      // It's not a secret as it's the original value
      throw ExceptionWrapper.builder(e, "The value " + value + " of " + this.getAttributeMetadata() + " cannot be converted to a " + valueClazz.getSimpleName() + ". Error: " + e.getMessage())
        .setPosition(ExceptionWrapper.ContextPosition.FIRST)
        .buildAsRuntimeException()
        ;
    }

  }

  @Override
  public String toString() {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
      .append(this.attributeEnum.toString())
      .append(" = ");
    if (this.valueProvider != null) {
      stringBuilder.append("value provider");
    } else {

      Object rawValue = this.rawValue;
      if (rawValue != null) {
        stringBuilder.append(rawValue);
      } else if (this.plainValue != null) {

        /**
         * No clear value in the log
         */
        stringBuilder.append("secret");

      } else {

        stringBuilder.append("no value");

      }

    }
    return stringBuilder.toString();

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
  public <T> T getValueOrDefaultCastAs(Class<T> clazz) throws CastException {
    Object object = this.getValueOrDefault();
    return Casts.cast(object, clazz);
  }

  /**
   * Same as {@link #getValueOrDefaultCastAs(Class)} but without compile exception
   * for the case when we know that there is a value of this type
   */
  @SuppressWarnings("unused")
  public <T> T getValueOrDefaultCastAsSafe(Class<T> clazz) {

    try {
      return getValueOrDefaultCastAs(clazz);
    } catch (CastException e) {
      throw new RuntimeException(e);
    }

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

  /**
   * @return the plain value or the provider or null
   * It's used in templating to pass template variable
   * without computing each attribute
   */
  public Object getPublicValueOrProvider() {
    Object publicValue = this.getRawValue();
    if (publicValue != null) {
      return publicValue;
    }
    if (this.valueProvider != null) {
      return this.valueProvider;
    }
    return null;
  }


  public Object getValueOrNull() {
    try {
      return this.getValue();
    } catch (NoValueException e) {
      return null;
    }
  }


  /**
   * @return the string value or the empty string if null
   * By default, it was "null", not the empty string
   */
  public String getValueOrDefaultAsStringNotNull() {

    Object valueOrDefault = getValueOrDefault();
    if (valueOrDefault == null) {
      return "";
    }
    return String.valueOf(valueOrDefault);

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

  /**
   * @param value - the raw or public value (maybe a string, a list or a map), found in a file
   *              {@link #setPlainValue(Object)} will set it also if the raw value not set
   *              This value is:
   *              * used to create the {@link ConfVault}
   *              * printed to the console {@link #getPublicValue()}
   */
  public Attribute setRawValue(Object value) {
    this.rawValue = value;
    return this;
  }

  /**
   * @return a public value, ie the value suitable for the console (ie hiding the secrets)
   * This function will run the {@link #setValueProvider(Supplier)}
   * See {@link #getPublicValueOrProvider()} if you want them before computation
   */
  public String getPublicValue() {

    Object originalValue = this.getRawValue();
    if (originalValue == null) {
      originalValue = this.getValueOrDefault();
      if ((isTabulifySecretAttribute || isOsSecret) && originalValue != null) {
        originalValue = NOT_SHOWN_SECRET;
      }
    }
    if (originalValue == null) {
      return "";
    }
    boolean isCollection = Collection.class.isAssignableFrom(originalValue.getClass());
    if (isCollection) {
      return String.join(", ", Casts.castToNewListSafe(originalValue, String.class));
    }
    if (originalValue.getClass().isArray()) {
      try {
        return String.join(", ", Casts.castToArray(originalValue, String.class));
      } catch (CastException e) {
        throw new InternalException("The attribute " + this + " has an array value that could not be casted to an array of string. Error:" + e.getMessage(), e);
      }
    }

    return originalValue.toString();

  }


  public boolean isValueProviderValue() {
    return valueProvider != null;
  }

}
