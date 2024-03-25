package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An email (The object stored in the file system)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Email   {


  protected String subject;

  protected Map<String, Object> body = new HashMap<>();

  protected Map<String, Object> preview = new HashMap<>();

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Email () {
  }

  /**
  * @return subject The subject of the email
  */
  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  /**
  * @param subject The subject of the email
  */
  @SuppressWarnings("unused")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
  * @return body The body of the email (Rich Slate Format)
  */
  @JsonProperty("body")
  public Map<String, Object> getBody() {
    return body;
  }

  /**
  * @param body The body of the email (Rich Slate Format)
  */
  @SuppressWarnings("unused")
  public void setBody(Map<String, Object> body) {
    this.body = body;
  }

  /**
  * @return preview The preview of the email (Rich Slate Format)
  */
  @JsonProperty("preview")
  public Map<String, Object> getPreview() {
    return preview;
  }

  /**
  * @param preview The preview of the email (Rich Slate Format)
  */
  @SuppressWarnings("unused")
  public void setPreview(Map<String, Object> preview) {
    this.preview = preview;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Email email = (Email) o;
    return

            Objects.equals(subject, email.subject) && Objects.equals(body, email.body) && Objects.equals(preview, email.preview);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, body, preview);
  }

  @Override
  public String toString() {
    return subject;
  }

}
