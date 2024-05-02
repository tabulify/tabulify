package net.bytle.vertx.guid;

import net.bytle.exception.InternalException;

/**
 * A guid is a string identifier
 * Example: usr-xxmnmnsd
 */
public abstract class Guid {


  @Override
  public String toString() {

    // A guid is serialized through Jackson as we hash it with a secret
    throw new InternalException("To serialize a guid, you should use Jackson");

  }

  /**
   *
   * @return a toString that shows the local ids
   */
  public abstract String toStringLocalIds();


}
