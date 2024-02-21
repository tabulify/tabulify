package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A post mailing object
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListMailingPost   {


  protected String name;

  protected String subject;

  protected String authorGuid;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListMailingPost () {
  }

  /**
  * @return name The name of the mailing
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The name of the mailing
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return subject The subject of the mailing
  */
  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  /**
  * @param subject The subject of the mailing
  */
  @SuppressWarnings("unused")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
  * @return authorGuid The author of the mailing
  */
  @JsonProperty("authorGuid")
  public String getAuthorGuid() {
    return authorGuid;
  }

  /**
  * @param authorGuid The author of the mailing
  */
  @SuppressWarnings("unused")
  public void setAuthorGuid(String authorGuid) {
    this.authorGuid = authorGuid;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListMailingPost listMailingPost = (ListMailingPost) o;
    return

            Objects.equals(name, listMailingPost.name) && Objects.equals(subject, listMailingPost.subject) && Objects.equals(authorGuid, listMailingPost.authorGuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, subject, authorGuid);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
