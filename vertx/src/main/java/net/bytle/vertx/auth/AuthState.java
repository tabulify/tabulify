package net.bytle.vertx.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import net.bytle.type.Base64Utility;

/**
 * A pojo to:
 * * define authentication state
 * * encode the auth state for OAuth
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthState {


  private static final String LIST_GUID = "listGuid";
  private static final String RANDOM_VALUE = "randomValue";
  private final JsonObject jsonObject;

  public static AuthState createFromStateString(String state) {
    String jsonState = Base64Utility.base64UrlStringToString(state);
    return new AuthState(new JsonObject(jsonState));
  }


  /**
   *
   */
  public AuthState(JsonObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public static AuthState createEmpty() {
    return new AuthState(new JsonObject());
  }

  /**
   * If the registration occurs for a list
   *
   * @param listGuid - the list id
   */
  public void setListGuid(String listGuid) {
    this.jsonObject.put(LIST_GUID,listGuid);
  }

  public String getListGuid() {
    return this.jsonObject.getString(LIST_GUID);
  }


  public String getRandomValue() {
    return this.jsonObject.getString(RANDOM_VALUE);
  }

  public void setRandomValue(String randomValue) {

    this.jsonObject.put(RANDOM_VALUE,randomValue);
  }

  public String toUrlValue() {
    String jsonString = JsonObject.mapFrom(this).toString();
    return Base64Utility.stringToBase64UrlString(jsonString);
  }

}
