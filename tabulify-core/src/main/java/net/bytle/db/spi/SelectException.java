package net.bytle.db.spi;

/**
 * An exception when the select stream cannot be created
 */
public class SelectException extends Exception {


  public SelectException(String message, Exception e) {
    super(message, e);
  }


}
