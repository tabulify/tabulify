package net.bytle.tower.eraldy.app.memberapp.implementer.util;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NullValueException;
import net.bytle.type.Base64Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * We pass data to the frontend via cookie
 * * the auth realm: for the
 * * data: for all confirmation page and the registration page
 * <p>
 * The type should be serializable to Json (ie Map<String, Object> or a pojo)
 */
public class FrontEndCookie<T> {

  private static final Logger LOGGER = LogManager.getLogger(FrontEndCookie.class);

  private static final String COOKIE_DATA_NAME = "data";
  private final RoutingContext routingContext;
  private final String cookieName;
  private String cookiePath;
  private final Class<? extends T> aClass;

  public FrontEndCookie(RoutingContext routingContext, String cookieName, Class<T> aClass) {
    this.routingContext = routingContext;
    this.cookieName = cookieName;
    this.aClass = aClass;
  }

  /**
   * @param variables - it should be serializable to Json (ie Map<String, Object> or a pojo)
   * @return the cookie data
   */
  public static Cookie getCookieData(Object variables) {
    JsonObject data = JsonObject.mapFrom(variables);
    return Cookie.cookie(COOKIE_DATA_NAME, Base64Utility.stringToBase64UrlString(data.toString()))
      .setHttpOnly(false);
  }


  public static <T> FrontEndCookie<T> createCookieData(RoutingContext routingContext, Class<T> aClass) {
    return new Conf<>(routingContext, COOKIE_DATA_NAME, aClass)
      .build();
  }


  public static <T> Conf<T> conf(RoutingContext routingContext, String cookieName, Class<T> aClass) {
    return new Conf<>(routingContext, cookieName, aClass);
  }

  /**
   * @param value - the value should be serializable to Json (ie Map<String, Object> or a pojo)
   */
  public void setValue(T value) {
    JsonObject json = JsonObject.mapFrom(value);
    Cookie cookie = Cookie.cookie(this.cookieName, Base64Utility.stringToBase64UrlString(json.toString()))
      .setHttpOnly(false)
      .setSameSite(CookieSameSite.STRICT);
    if (this.cookiePath != null) {
      cookie.setPath(this.cookiePath);
    }
    this.routingContext.response().addCookie(cookie);
  }

  public T getValue() throws NullValueException {
    Cookie value = this.routingContext.request().getCookie(this.cookieName);
    if (value == null) {
      throw new NullValueException();
    }
    String rawValue = value.getValue();
    try {
      String json = Base64Utility.base64UrlStringToString(rawValue);
      return (new JsonObject(json)).mapTo(this.aClass);
    } catch (Exception e) {
      // should not occur but in dev, it may
      LOGGER.error("We were unable to decode the frontend cookie (cookieName: " + this.cookieName + ", value: " + rawValue + ")");
      throw new NullValueException();
    }
  }

  public static class Conf<T> {
    private final RoutingContext routingContext;
    private final String cookieName;
    private final Class<T> aClass;
    private String cookiePath;

    public Conf(RoutingContext routingContext, String cookieName, Class<T> aClass) {
      this.routingContext = routingContext;
      this.cookieName = cookieName;
      this.aClass = aClass;
    }

    public Conf<T> setPath(String path) {
      this.cookiePath = path;
      return this;
    }

    public FrontEndCookie<T> build() {
      FrontEndCookie<T> frontendCookie = new FrontEndCookie<>(routingContext, cookieName, this.aClass);
      frontendCookie.cookiePath = cookiePath;
      return frontendCookie;
    }
  }
}
