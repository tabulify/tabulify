package net.bytle.tower.eraldy.model.manual;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Email Doc
 * An email
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailDoc {

  protected String subject;

  protected String preview;



  private String body;



  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public EmailDoc() {
  }

  /**
   * @return guid The public id (derived from the database/local id)
   */
  @JsonProperty("guid")
  public String getSubject() {
    return subject;
  }

  /**
   * @param subject The public id (derived from the database/local id)
   */
  @SuppressWarnings("unused")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * @return name The email preview
   */
  @JsonProperty("preview")
  public String getPreview() {
    return preview;
  }

  /**
   * @param preview The email preview
   */
  @SuppressWarnings("unused")
  public void setPreview(String preview) {
    this.preview = preview;
  }


  @JsonProperty("body")
  public String getBody() {
    return this.body;
  }

  @SuppressWarnings("unused")
  public void setBody(String body) {
    this.body = body;
  }


}
