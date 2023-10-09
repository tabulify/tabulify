package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Objects;

/**
 * the csp report
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CspReport extends HashMap<String, String>  {

  private String documentUri;
  private String referrer;
  private String blockedUri;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public CspReport () {
  }

  /**
  * @return documentUri The result of executing Strip URL for use in reports on violation’s url.
  */
  @JsonProperty("document-uri")
  public String getDocumentUri() {
    return documentUri;
  }

  /**
  * @param documentUri The result of executing Strip URL for use in reports on violation’s url.
  */
  @SuppressWarnings("unused")
  public void setDocumentUri(String documentUri) {
    this.documentUri = documentUri;
  }

  /**
  * @return referrer The result of executing Strip URL for use in reports on violation’s referrer.
  */
  @JsonProperty("referrer")
  public String getReferrer() {
    return referrer;
  }

  /**
  * @param referrer The result of executing Strip URL for use in reports on violation’s referrer.
  */
  @SuppressWarnings("unused")
  public void setReferrer(String referrer) {
    this.referrer = referrer;
  }

  /**
  * @return blockedUri The result of executing Strip URL for use in reports on violation’s referrer.
  */
  @JsonProperty("blocked-uri")
  public String getBlockedUri() {
    return blockedUri;
  }

  /**
  * @param blockedUri The result of executing Strip URL for use in reports on violation’s referrer.
  */
  @SuppressWarnings("unused")
  public void setBlockedUri(String blockedUri) {
    this.blockedUri = blockedUri;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CspReport cspReport = (CspReport) o;
    return super.equals(o) && Objects.equals(documentUri, cspReport.documentUri) &&
        Objects.equals(referrer, cspReport.referrer) &&
        Objects.equals(blockedUri, cspReport.blockedUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), documentUri, super.hashCode(), referrer, super.hashCode(), blockedUri);
  }

  @Override
  public String toString() {
    return "class CspReport {\n" +
    "    " + toIndentedString(super.toString()) + "\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
