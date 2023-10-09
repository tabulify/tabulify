package net.bytle.tower.util;

@SuppressWarnings("unused")
public class HttpStatus {


  public static final int SUCCESS_NO_CONTENT = 204;

  /**
   * Used when a user is logged in
   * but does not have the authorization
   */
  public static final int NOT_AUTHORIZED = 401;

  public static final int NOT_FOUND = 404;

  public static final int REDIRECT = 302;


  /**
   * Send by CSRF check for instance
   */
  public static final int FORBIDDEN = 403;

  public static final int BAD_REQUEST = 400;

  public static final int INTERNAL_ERROR = 500;

  /**
   * Tracking redirect
   * (Does not work in Chrome for a POST as it does not perform a GET)
   */
  public static final int REDIRECT_SEE_OTHER_URI = 303;

}
