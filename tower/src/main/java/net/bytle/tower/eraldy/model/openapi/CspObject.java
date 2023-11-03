package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A CSP violation report object
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CspObject   {


  protected CspReport cspReport;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public CspObject () {
  }

  /**
  * @return cspReport
  */
  @JsonProperty("csp-report")
  public CspReport getCspReport() {
    return cspReport;
  }

  /**
  * @param cspReport Set cspReport
  */
  @SuppressWarnings("unused")
  public void setCspReport(CspReport cspReport) {
    this.cspReport = cspReport;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CspObject cspObject = (CspObject) o;
    return Objects.equals(cspReport, cspObject.cspReport);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cspReport);
  }

  @Override
  public String toString() {
    return "class CspObject {\n" +

    "    cspReport: " + toIndentedString(cspReport) + "\n" +
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
