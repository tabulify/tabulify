package net.bytle.type;

public class Typess {

  /**
   * @param object - the object to cast
   * @param clazz - the class to cast
   * @param <T>
   * @return null if the object is null, throw an exception if the class is not the expected one
   * the object to the asked clazz
   */
  @SuppressWarnings("unchecked")
  public static <T> T safeCast(Object object, Class<T> clazz) {
    if (object == null) {
      return null;
    } else {
      if (object.getClass().equals(clazz)) {
        return (T) object;
      } else {
        throw new RuntimeException("The class of the object is " + object.getClass() + " but we expect a (" + clazz + ") (Value: " + object + ")");
      }
    }
  }


}
