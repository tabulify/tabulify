package net.bytle.tower.eraldy.model.manual;

import io.vertx.core.json.JsonObject;
import net.bytle.type.Strings;

/**
 * Represent the variables used in an email
 */
public class EmailTemplateVariables {


  public static EmailTemplateVariables create() {
    return new EmailTemplateVariables();
  }

  JsonObject variables = new JsonObject();

  public EmailTemplateVariables setRecipientGivenName(String recipientName) {
    variables.put("0", Strings
      .createFromString(recipientName)
      .toFirstLetterCapitalCase()
      .toString()
    );
    return this;
  }

  public JsonObject getVariables() {
    return variables;
  }


}
