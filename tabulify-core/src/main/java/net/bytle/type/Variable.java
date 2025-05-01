package net.bytle.type;

import net.bytle.exception.CastException;
import net.bytle.exception.NoValueException;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A super set of a key pair value
 * adding functionality such:
 * * as conf/secret via {@link #setCipherValue(String)} and {@link #setPlainValue(Object)} (Object)}
 * * key case independence (via {@link Attribute} that uses a {@link KeyNormalizer})
 */
public class Variable implements Comparable<Variable> {


  private Attribute attribute;

  /**
   * Origin of the value
   */
  private final Origin origin;

  /**
   * A value to decipher (encrypted or env expression)
   */
  private String cipherValue;

  /**
   * A plain value (decrypted if needed).
   * If this value was decrypted, the {@link #cipherValue} is not null
   */
  private Object plainValue;


  /**
   * A function that gives the value
   */
  private Supplier<?> valueProvider;

  private Variable(Attribute attribute, Origin origin) {

    this.attribute = attribute;
    if (origin == null) {
      throw new IllegalArgumentException("The origin of the variable (" + this + ") was null, it should not");
    }
    // origin is important for security reason, that's why it is in the constructor
    this.origin = origin;

  }

  public static Variable create(String name, Origin origin) {
    return createWithClass(name, origin, String.class);
  }


  /**
   * Utility class of {@link #createWithClassAndDefault(String, Origin, Class, Object)}
   * where the default value is null
   */
  public static Variable createWithClass(String name, Origin origin, Class<?> clazz) {
    return createWithClassAndDefault(name, origin, clazz, null);
  }

  /**
   * Variable may be instantiated at runtime without knowing the name at compile time.
   * For instance,
   * * the properties of a jdbc driver. We don't master that
   * * dynamic variable such as the backref reference of a regexp $1, ...
   * So we need to be able to create a variable by name
   */
  public static Variable createWithClassAndDefault(String name, Origin origin, Class<?> clazz, Object defaultValue) {


    Attribute attributeFromName = new Attribute() {


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

    return new Variable(attributeFromName, origin);
  }

  public static Variable create(Attribute attribute, Origin origin) {

    return new Variable(attribute, origin);
  }


  public Origin getOrigin() {
    return this.origin;
  }


  public String getCipherValue() {
    return this.cipherValue;
  }

  /**
   * @return the value to be used in the application in clear and cast as specified by the {@link Attribute#getValueClazz()}
   */
  public Object getValueOrDefault() throws NoValueException {

    Object valueOrDefaultNonCasted = this.getValueOrDefaultNonCasted();

    Class<?> valueClazz = this.attribute.getValueClazz();

    try {
      return Casts.cast(valueOrDefaultNonCasted, valueClazz);
    } catch (CastException e) {
      /**
       * TODO: should be when setting the value
       */
      throw new ClassCastException(e.getMessage());
    }

  }

  public Object getValueOrDefaultNonCasted() throws NoValueException {
    try {

      return this.getValue();

    } catch (NoValueException e) {

      Object value = this.attribute.getDefaultValue();
      if (value != null) {
        return value;
      }

      throw new NoValueException("No value or default value found");
    }

  }


  @SuppressWarnings("unused")
  public Variable setPlainValue(Object value) {
    Class<?> valueClazz = this.attribute.getValueClazz();
    if (valueClazz == null) {
      throw new ClassCastException("The class of the attribute " + this.attribute + " should not be null");
    }
    try {
      this.plainValue = Casts.cast(value, valueClazz);
    } catch (CastException e) {
      // It's not a secret as it's the original value
      throw new ClassCastException("The value " + value + " of " + this.getAttribute() + " is not a " + valueClazz);
    }
    return this;
  }

  @Override
  public String toString() {
    /**
     * No clear value in the log
     */
    return this.attribute.toString() + " = " + Strings.createFromObjectNullSafe(this.cipherValue);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return this.attribute.equals(((Variable) o).attribute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.attribute.hashCode());
  }

  public Attribute getAttribute() {
    return this.attribute;
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
    if (this.cipherValue != null) {
      return this.cipherValue;
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
  public Variable setValueProvider(Supplier<?> valueProvider) {
    this.valueProvider = valueProvider;
    return this;
  }

  /**
   * @return if the value is derived (ie provided)
   */
  public Boolean isValueProvider() {
    return this.valueProvider != null;
  }


  public void setAttribute(Attribute attribute) {
    this.attribute = attribute;
  }


  @Override
  public int compareTo(Variable o) {
    return this.attribute.toString().compareTo(o.attribute.toString());
  }

  public Variable setCipherValue(String string) {
    this.cipherValue = string;
    return this;
  }
}
