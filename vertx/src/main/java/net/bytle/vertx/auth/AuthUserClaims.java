package net.bytle.vertx.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

/**
 * An auth user object to bridge with the Vertx User and the model user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthUserClaims {

  private String subjectGuid;
  private String audienceRealmGuid;
  private String audienceHandle;
  private String subjectHandle;
  private String email;

  @JsonProperty("subject")
  public String getSubjectGuid() {
    return this.subjectGuid;
  }

  /**
   * @return the audience (ie realm identifier)
   */
  @JsonProperty("audience")
  public String getAudienceRealmGuid() {
    return this.audienceRealmGuid;
  }

  @JsonProperty("email")
  public String getEmail() {
    return this.email;
  }

  @JsonProperty("handle")
  public String getHandle() {
    return this.subjectHandle;
  }

  public String getAudienceHandle() {
    return this.audienceHandle;
  }

  public void setSubject(String guid) {
    this.subjectGuid = guid;
  }

  public void setAudience(String guid) {
    this.audienceRealmGuid = guid;
  }

  public void setAudienceHandle(String handle) {
    this.audienceHandle = handle;
  }

  public void setSubjectHandle(String handle) {
    this.subjectHandle = handle;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public User toVertxUser(){

    /**
     * Not finish
     * A second argument attributes can be provided to provide extra metadata for later usage.
     * One example are the following attributes:
     * * exp - Expires at in seconds.
     * * iat - Issued at in seconds.
     * * nbf - Not before in seconds.
     * * leeway - clock drift leeway in seconds.
     * <p>
     * The first 3 control how the expired method will compute the expiration of the user,
     * the last can be used to allow clock drifting compensation while computing the expiration time.
     */
    JsonObject principal = new JsonObject();
    principal.put("userHandle",this.getHandle());
    return  User.create(principal);

  }

}
