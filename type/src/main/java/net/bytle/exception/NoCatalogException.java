package net.bytle.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoCatalogException extends Exception {

  public NoCatalogException() {
    super();
  }

  public NoCatalogException(String s) {
    super(s);
  }

}
