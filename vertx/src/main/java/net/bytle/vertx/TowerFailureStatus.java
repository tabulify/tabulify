package net.bytle.vertx;

import net.bytle.exception.NullValueException;

public interface TowerFailureStatus {

  int getStatusCode();

  String getMessage() throws NullValueException;

  String getType();

}
