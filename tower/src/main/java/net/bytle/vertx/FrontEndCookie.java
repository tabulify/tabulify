package net.bytle.vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.Base64Utility;

/**
 * A cookie utility function to create cookie
 * This cookie class allows to encode the data in json and base64.
 * <p>
 * We pass and get data to the frontend via cookie
 * * the auth cli: for the auth member app
 * * data: for all confirmation page and the registration page
 * <p>
 * The type should be serializable to Json (ie Map<String, Object> or a pojo)
 */
public class FrontEndCookie<T> {

  private static final String DEFAULT_COOKIE_DATA_NAME = "data";
  private final String cookieName;
  private final boolean httpOnly;
  private final CookieSameSite sameSitePolicy;
  private final String cookiePath;
  private final Class<? extends T> aClass;
  private final ObjectMapper jsonMapper;

  /**
   * With the easiness to create certificate on dev,
   * the cookie should always be sent on HTTPS
   */
  @SuppressWarnings("FieldCanBeLocal")
  private boolean COOKIE_HTTPS_SECURE = true;

  private FrontEndCookie(Conf<T> config) {
    this.cookieName = config.cookieName;
    this.aClass = config.aClass;
    this.cookiePath = config.cookiePath;
    this.jsonMapper = config.jsonMapper;
    this.httpOnly = config.httpOnly;
    this.sameSitePolicy = config.sameSitePolicy;
  }

  /**
   * @param variables - it should be serializable to Json (ie Map<String, Object> or a pojo)
   * @return the cookie data
   */
  public static Cookie getCookieData(Object variables) {
    JsonObject data = JsonObject.mapFrom(variables);
    return Cookie.cookie(DEFAULT_COOKIE_DATA_NAME, Base64Utility.stringToBase64UrlString(data.toString()))
      .setHttpOnly(false)
      .setSecure(true)
      ;
  }


  public static <T> FrontEndCookie<T> createCookieData(Class<T> aClass) {
    return new Conf<>(DEFAULT_COOKIE_DATA_NAME, aClass)
      .setHttpOnly(false)
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
    if (value instanceof String) {
      stringValue = (String) value;
    } else {
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
    }


    COOKIE_HTTPS_SECURE = true;
    Cookie cookie = Cookie.cookie(this.cookieName, Base64Utility.stringToBase64UrlString(stringValue))
      .setHttpOnly(httpOnly)
      .setSecure(COOKIE_HTTPS_SECURE)
      .setSameSite(this.sameSitePolicy);
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
    String clearString;
    try {
      clearString = Base64Utility.base64UrlStringToString(rawValue);
    } catch (Exception e) {
      throw new CastException("We were unable to decode the cookie (cookieName: " + this.cookieName + ") from base64 with the value: (" + rawValue + ")");
    }

    /**
     * Simple string value
     */
    if (this.aClass.equals(String.class)) {
      return this.aClass.cast(clearString);
    }

    /**
     * Decode Json to Object
     * We cannot use the JsonMapper of the API
     * because they exclude the Guid which is the Jackson unique identifier (ie JsonIdentityInfo)
     * This identifier is needed for the creation of the object
     */
    try {
      return (new JsonObject(clearString)).mapTo(this.aClass);
    } catch (Exception e) {
      // should not occur but in dev, it may
      throw new CastException("We were unable to decode the cookie (cookieName: " + this.cookieName + ") to the class (" + this.aClass + ") with the value: " + clearString + ")", e);
    }
  }

  public static class Conf<T> {
    private final String cookieName;
    private final Class<T> aClass;
    private String cookiePath;
    private ObjectMapper jsonMapper;
    private boolean httpOnly = true;
    private CookieSameSite sameSitePolicy = CookieSameSite.STRICT;

    public Conf(String cookieName, Class<T> aClass) {
      this.cookieName = cookieName;
      this.aClass = aClass;
    }

    public Conf<T> setPath(String path) {
      this.cookiePath = path;
      return this;
    }

    public FrontEndCookie<T> build() {
      return new FrontEndCookie<>(this);
    }

    public Conf<T> setJsonMapper(ObjectMapper jsonMapper) {
      this.jsonMapper = jsonMapper;
      return this;
    }

    public Conf<T> setHttpOnly(boolean httpOnly) {
      this.httpOnly = httpOnly;
      return this;
    }

    public Conf<T> setSameSite(CookieSameSite policy) {
      this.sameSitePolicy = policy;
      return this;
    }

    /**
     * In dev, localhost:8083, the cookie is set on localhost
     * and is not send back if the policy is not strict
     * This function is a utility function just for that.
     */
    public Conf<T> setSameSiteStrictInProdAndLaxInDev() {
      if(JavaEnvs.IS_DEV){
        return this.setSameSite(CookieSameSite.LAX);
      }
      return this.setSameSite(CookieSameSite.STRICT);
    }
  }
}
