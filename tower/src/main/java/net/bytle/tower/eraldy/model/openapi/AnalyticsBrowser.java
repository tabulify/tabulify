package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * Track browser specific information
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsBrowser   {


  protected URI uri;

  protected URI referrer;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsBrowser () {
  }

  /**
  * @return uri the address uri in the browser
  */
  @JsonProperty("uri")
  public URI getUri() {
    return uri;
  }

  /**
  * @param uri the address uri in the browser
  */
  @SuppressWarnings("unused")
  public void setUri(URI uri) {
    this.uri = uri;
  }

  /**
  * @return referrer The `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @JsonProperty("referrer")
  public URI getReferrer() {
    return referrer;
  }

  /**
  * @param referrer The `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @SuppressWarnings("unused")
  public void setReferrer(URI referrer) {
    this.referrer = referrer;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsBrowser analyticsBrowser = (AnalyticsBrowser) o;
    return

            Objects.equals(uri, analyticsBrowser.uri) && Objects.equals(referrer, analyticsBrowser.referrer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, referrer);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
