package net.bytle.vertx;


import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;

/**
 * A failure status with HTTP status code mapping
 * (not that NOT_AUTHORIZED and NOT_LOGGED_IN have the HTTP status code)
 */
public enum TowerFailureStatusEnum implements TowerFailureStatus {


  SUCCESS_NO_CONTENT_204(204, "Success, the response has no body"),

  /**
   * Used when a user is not logged in
   * Known in HTTP as not authorized
   * If logged in, use {@link #NOT_AUTHORIZED_403}
   */
  NOT_LOGGED_IN_401(401, "Not logged in"),


  /**
   * When the resource is not found (user, document, ...)
   */
  NOT_FOUND_404(404, "Not found"),

  REDIRECT_302(302, "Redirect, the resource has moved"),


  /**
   * Used when the user is logged in
   * but does not have the proper authorization
   * Known in HTTP has forbidden
   * (Send by CSRF check for instance
   */
  NOT_AUTHORIZED_403(403, "Not authorized, forbidden (user logged in)"),

  /**
   * see also {@link io.netty.handler.codec.http.HttpResponseStatus#BAD_REQUEST}
   */
  BAD_REQUEST_400(400, "Bad request"),

  INTERNAL_ERROR_500(500, "Internal error (our fault)"),

  /**
   * Tracking redirect
   * (Does not work in Chrome for a POST as it does not perform a GET)
   */
  REDIRECT_SEE_OTHER_URI_303(303, "Redirect, see other uri"),
  /**
   * When the user click on an expired link
   */
  LINK_EXPIRED(400, "The link is expired");

  private final int httpStatusCode;
  private final String message;

  TowerFailureStatusEnum(int httpStatusCode, String message) {

    this.httpStatusCode = httpStatusCode;
    this.message = message;

  }

  public static TowerFailureStatusEnum fromHttpStatusCode(int httpStatusCode) throws NotFoundException {
    for (TowerFailureStatusEnum towerFailureStatusEnum : values()) {
      if (towerFailureStatusEnum.httpStatusCode == httpStatusCode) {
        return towerFailureStatusEnum;
      }
    }
    throw new NotFoundException();
  }

  public int getStatusCode() {
    return this.httpStatusCode;
  }

  public String getMessage() throws NullValueException {
    if (message == null) {
      return this.name().toLowerCase();
    }
    return message;
  }
}
