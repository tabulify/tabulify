package net.bytle.vertx;

import net.bytle.exception.NullValueException;

public interface TowerFailureType {

  int getStatusCode();

  String getMessage() throws NullValueException;

  String getType();

}
