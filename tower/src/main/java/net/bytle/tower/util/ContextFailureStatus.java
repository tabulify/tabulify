package net.bytle.tower.util;

import net.bytle.vertx.HttpStatus;

public enum ContextFailureStatus {

  /**
   * Used when a user is logged in
   * but does not have the authorization
   */
  NOT_AUTHORIZED(HttpStatus.NOT_AUTHORIZED);

  private final int httpStatus;

  ContextFailureStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

}
