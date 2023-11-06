package net.bytle.vertx;


import io.netty.handler.codec.http.HttpResponseStatus;
import net.bytle.exception.NotFoundException;

/**
 * An HTTP status that permits more
 * status error for the same error code
 */
public enum HttpStatus {


  SUCCESS_NO_CONTENT(204),

  /**
   * Used when a user is logged in
   * but does not have the authorization
   */
  NOT_AUTHORIZED(401),

  NOT_LOGGED_IN(401),

  NOT_FOUND(404),

  REDIRECT( 302),


  /**
   * Send by CSRF check for instance
   */
  FORBIDDEN(403),

  BAD_REQUEST(HttpResponseStatus.BAD_REQUEST.code()),

  INTERNAL_ERROR (500),

  /**
   * Tracking redirect
   * (Does not work in Chrome for a POST as it does not perform a GET)
   */
  REDIRECT_SEE_OTHER_URI (303),
  /**
   * Used when the HTTP status is unknown
   */
  UNKNOWN_STATUS(500);

  private final int httpStatusCode;

  HttpStatus(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  public static HttpStatus fromHttpStatusCode(int httpStatusCode) throws NotFoundException {
    for(HttpStatus httpStatus: values()){
      if(httpStatus.httpStatusCode == httpStatusCode){
        return httpStatus;
      }
    }
    throw new NotFoundException();
  }

  public int httpStatusCode() {
    return this.httpStatusCode;
  }
}
