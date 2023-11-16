package net.bytle.vertx;


import net.bytle.exception.NotFoundException;

/**
 * An HTTP status that permits more
 * status error for the same HTTP status code
 * (ie NOT_AUTHORIZED = NOT_LOGGED_IN HTTP status code)
 */
public enum HttpStatusEnum implements HttpStatus {


  SUCCESS_NO_CONTENT_204(204),

  /**
   * Used when a user is logged in
   * but does not have the authorization
   */
  NOT_AUTHORIZED_401(401),

  NOT_LOGGED_IN_401(401),

  NOT_FOUND_404(404),

  REDIRECT_302( 302),


  /**
   * Send by CSRF check for instance
   */
  FORBIDDEN_403(403),

  /**
   * see also {@link io.netty.handler.codec.http.HttpResponseStatus#BAD_REQUEST}
   */
  BAD_REQUEST_400(400),

  INTERNAL_ERROR_500(500),

  /**
   * Tracking redirect
   * (Does not work in Chrome for a POST as it does not perform a GET)
   */
  REDIRECT_SEE_OTHER_URI_303(303),
  /**
   * When the user click on a expired link
   */
  LINK_EXPIRED(400);

  private final int httpStatusCode;

  HttpStatusEnum(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  public static HttpStatusEnum fromHttpStatusCode(int httpStatusCode) throws NotFoundException {
    for(HttpStatusEnum httpStatusEnum : values()){
      if(httpStatusEnum.httpStatusCode == httpStatusCode){
        return httpStatusEnum;
      }
    }
    throw new NotFoundException();
  }

  public int getStatusCode() {
    return this.httpStatusCode;
  }
}
