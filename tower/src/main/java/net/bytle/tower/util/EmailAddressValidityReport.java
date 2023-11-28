package net.bytle.tower.util;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class EmailAddressValidityReport {
  private final String email;
  private final Map<String, String> successMessages = new HashMap<>();
  private final Map<String, String> errorMessages = new HashMap<>();
  public EmailAddressValidityReport(String email) {
    this.email = email;
  }

  public EmailAddressValidityReport addError(String type,String message) {
    this.errorMessages.put(type,message);
    return this;
  }

  public EmailAddressValidityReport addSuccess(String checkType,String message) {
    this.successMessages.put(checkType,message);
    return this;
  }

  public boolean isValid() {
    return !(this.errorMessages.size()>0);
  }

  public JsonObject toJsonObject() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("valid", this.isValid());
    jsonObject.put("email", this.email);
    JsonObject jsonObjectMessage = new JsonObject();
    jsonObject.put("results", jsonObjectMessage);
    jsonObjectMessage.put("errors", this.errorMessages);
    jsonObjectMessage.put("success", this.successMessages);
    return jsonObject;
  }
}
