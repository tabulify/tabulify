package net.bytle.tower.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import net.bytle.tower.eraldy.model.openapi.RegistrationList;
import net.bytle.type.Base64Utility;

/**
 * A pojo to encode the state
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthExternalState {


  private String randomValue;
  private String listGuid;

  public static OAuthExternalState createFromStateString(String state) {
    String jsonState = Base64Utility.base64UrlStringToString(state);
    return new JsonObject(jsonState).mapTo(OAuthExternalState.class);
  }


  /**
   * Mandatory to serialize with {@link JsonObject#mapTo(Class)}
   */
  public OAuthExternalState() {
  }

  /**
   * If the registration occurs for a list
   *
   * @param listGuid - the {@link RegistrationList#getGuid()}
   */
  public void setListGuid(String listGuid) {
    this.listGuid = listGuid;
  }

  @JsonProperty("listGuid")
  public String getListGuid() {
    return listGuid;
  }

  @JsonProperty("randomValue")
  public String getRandomValue() {
    return randomValue;
  }

  public void setRandomValue(String randomValue) {
    this.randomValue = randomValue;
  }

  public String toStateString() {
    String jsonString = JsonObject.mapFrom(this).toString();
    return Base64Utility.stringToBase64UrlString(jsonString);
  }

}
