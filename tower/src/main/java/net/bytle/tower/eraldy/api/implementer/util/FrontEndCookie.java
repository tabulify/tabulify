package net.bytle.tower.eraldy.api.implementer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.type.Base64Utility;

/**
 * We pass data to the frontend via cookie
 * * the auth realm: for the
 * * data: for all confirmation page and the registration page
 * <p>
 * The type should be serializable to Json (ie Map<String, Object> or a pojo)
 */
public class FrontEndCookie<T> {

  private static final String COOKIE_DATA_NAME = "data";
  private final String cookieName;
  private String cookiePath;
  private final Class<? extends T> aClass;
  private ObjectMapper jsonMapper;

  public FrontEndCookie(String cookieName, Class<T> aClass) {
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


  public static <T> FrontEndCookie<T> createCookieData(Class<T> aClass) {
    return new Conf<>(COOKIE_DATA_NAME, aClass)
      .build();
  }


  public static <T> Conf<T> conf(String cookieName, Class<T> aClass) {
    return new Conf<>(cookieName, aClass);
  }

  /**
   * @param value - the value should be serializable to Json (ie Map<String, Object> or a pojo)
   */
  public void setValue(T value, RoutingContext routingContext) {

    String stringValue;
    if (jsonMapper == null) {
      JsonObject json = JsonObject.mapFrom(value);
      stringValue = json.toString();
    } else {
      try {
        stringValue = jsonMapper.writeValueAsString(value);
      } catch (JsonProcessingException e) {
        throw new InternalException(e);
      }
    }

    Cookie cookie = Cookie.cookie(this.cookieName, Base64Utility.stringToBase64UrlString(stringValue))
      .setHttpOnly(false)
      .setSameSite(CookieSameSite.STRICT);
    if (this.cookiePath != null) {
      cookie.setPath(this.cookiePath);
    }
    routingContext.response().addCookie(cookie);
  }

  public T getValue(RoutingContext routingContext) throws NullValueException, CastException {
    Cookie value = routingContext.request().getCookie(this.cookieName);
    if (value == null) {
      throw new NullValueException();
    }
    String rawValue = value.getValue();
    /**
     * Decode from base64
     */
    String json;
    try {
      json = Base64Utility.base64UrlStringToString(rawValue);
    } catch (Exception e) {
      throw new CastException("We were unable to decode the cookie (cookieName: " + this.cookieName + ") from base64 with the value: (" + rawValue + ")");
    }
    /**
     * Decode Json to Object
     * We cannot use the JsonMapper of the API
     * because they exclude the Guid which is the Jackson unique identifier (ie JsonIdentityInfo)
     * This identifier is needed for the creation of the object
     */
    try {
      return (new JsonObject(json)).mapTo(this.aClass);
    } catch (Exception e) {
      // should not occur but in dev, it may
      throw new CastException("We were unable to decode the cookie (cookieName: " + this.cookieName + ") to the class (" + this.aClass + ") with the value: " + json + ")", e);
    }
  }

  public static class Conf<T> {
    private final String cookieName;
    private final Class<T> aClass;
    private String cookiePath;
    private ObjectMapper jsonMapper;

    public Conf(String cookieName, Class<T> aClass) {
      this.cookieName = cookieName;
      this.aClass = aClass;
    }

    public Conf<T> setPath(String path) {
      this.cookiePath = path;
      return this;
    }

    public FrontEndCookie<T> build() {
      FrontEndCookie<T> frontendCookie = new FrontEndCookie<>(cookieName, this.aClass);
      frontendCookie.cookiePath = cookiePath;
      frontendCookie.jsonMapper = jsonMapper;
      return frontendCookie;
    }

    public Conf<T> setJsonMapper(ObjectMapper jsonMapper) {
      this.jsonMapper = jsonMapper;
      return this;
    }
  }
}
