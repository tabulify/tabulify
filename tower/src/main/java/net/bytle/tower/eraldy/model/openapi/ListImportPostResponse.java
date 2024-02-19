package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The successful response to the submission of a import job for a list
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListImportPostResponse   {


  protected String jobIdentifier;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListImportPostResponse () {
  }

  /**
  * @return jobIdentifier The job id
  */
  @JsonProperty("jobIdentifier")
  public String getJobIdentifier() {
    return jobIdentifier;
  }

  /**
  * @param jobIdentifier The job id
  */
  @SuppressWarnings("unused")
  public void setJobIdentifier(String jobIdentifier) {
    this.jobIdentifier = jobIdentifier;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListImportPostResponse listImportPostResponse = (ListImportPostResponse) o;
    return

            Objects.equals(jobIdentifier, listImportPostResponse.jobIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobIdentifier);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
