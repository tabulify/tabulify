package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A post mailing email object to update/create an email
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailingEmailPost   {


  protected String subject;

  protected String mediaType;

  protected String thirdType;

  protected String body;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public MailingEmailPost () {
  }

  /**
  * @return subject The email subject
  */
  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  /**
  * @param subject The email subject
  */
  @SuppressWarnings("unused")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
  * @return mediaType The email body media type (text/json for an AST)
  */
  @JsonProperty("mediaType")
  public String getMediaType() {
    return mediaType;
  }

  /**
  * @param mediaType The email body media type (text/json for an AST)
  */
  @SuppressWarnings("unused")
  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }

  /**
  * @return thirdType The structure of the media type (the AST for Json)
  */
  @JsonProperty("thirdType")
  public String getThirdType() {
    return thirdType;
  }

  /**
  * @param thirdType The structure of the media type (the AST for Json)
  */
  @SuppressWarnings("unused")
  public void setThirdType(String thirdType) {
    this.thirdType = thirdType;
  }

  /**
  * @return body The email body
  */
  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  /**
  * @param body The email body
  */
  @SuppressWarnings("unused")
  public void setBody(String body) {
    this.body = body;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MailingEmailPost mailingEmailPost = (MailingEmailPost) o;
    return

            Objects.equals(subject, mailingEmailPost.subject) && Objects.equals(mediaType, mailingEmailPost.mediaType) && Objects.equals(thirdType, mailingEmailPost.thirdType) && Objects.equals(body, mailingEmailPost.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, mediaType, thirdType, body);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
