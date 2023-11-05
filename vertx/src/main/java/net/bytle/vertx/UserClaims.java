package net.bytle.vertx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A user object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserClaims {

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

}
