package net.bytle.tower.eraldy.api.implementer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.model.openapi.ListUserPostBody;
import net.bytle.tower.util.Env;
import net.bytle.type.UriEnhanced;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 *
 */
public class ListRegistrationValidationToken {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ListRegistrationValidationToken.class);


  /**
   * The IP address used when the contact accessed the hosted signup form.
   */
  private String optInIp;

  /**
   * The time a contact accessed your signup form, if they used it to sign up.
   */
  private String optInTime;
  /**
   * The uri location of the opt-in form
   */
  private URI optInUri;
  private String userEmail;
  private final String listGuid;

  public ListRegistrationValidationToken(ListObject listObject) {
    this.listGuid = listObject.getGuid();
  }

  public static config config(ListObject listObject) {
    return new config(listObject);
  }

  @JsonProperty("optInIp")
  public String getOptInIp() {
    return optInIp;
  }

  @JsonProperty("optInTime")
  public String getOptInTime() {
    return optInTime;
  }

  @JsonProperty("optInUri")
  public URI getOptInUri() {
    return optInUri;
  }

  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  @JsonProperty("listGuid")
  public String getListGuid() {
    return listGuid;
  }

  public static class config {

    private final ListRegistrationValidationToken token;

    public ListRegistrationValidationToken build() {
      return token;
    }

    public config(ListObject listObject) {
      this.token = new ListRegistrationValidationToken(listObject);
    }

    public config setOptInIp(String optInIp) {
      this.token.optInIp = optInIp;
      return this;
    }

    public config setOptInTimeAsNow() {
      this.token.optInTime = Timestamp.createFromNowLocalSystem().toIsoString();
      return this;
    }

    public config addOptInContext(RoutingContext routingContext) {
      try {
        HttpServerRequest request = routingContext.request();
        token.optInIp = HttpRequestUtil.getRealRemoteClientIp(request);
        this.setOptInTimeAsNow();

        String refererHeaderName = "referer";
        String refererValue = request.getHeader(refererHeaderName);
        if (refererValue != null) {
          try {
            token.optInUri = UriEnhanced.createFromString(refererValue).toUri();
          } catch (IllegalStructure e) {
            if (Env.IS_DEV) {
              throw ValidationException.create("The referer header is not a valid uri. Error:" + e.getMessage(), refererHeaderName, refererValue);
            } else {
              LOGGER.warn("The referer header is not a valid uri (" + refererValue + "). Error:" + e.getMessage());
            }
          }
        }
      } catch (NotFoundException e) {
        throw new InternalException(e);
      }
      return this;
    }

    public config setFromListObject(ListUserPostBody listUserPost) {
      this.token.userEmail = listUserPost.getUserEmail();
      this.token.optInUri = listUserPost.getRedirectUri();
      return this;
    }
  }

}
