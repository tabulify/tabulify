package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * The UTM data See: * &lt;a href&#x3D;\&quot;https://datacadamia.com/marketing/analytics/utm\&quot;&gt;UTM&lt;/a&gt; * &lt;a href&#x3D;\&quot;https://ga-dev-tools.google/campaign-url-builder/\&quot;&gt;UTM builder&lt;/a&gt; * &lt;a href&#x3D;\&quot;https://support.google.com/analytics/answer/10917952\&quot;&gt;[GA4] URL builders: Collect campaign data with custom URLs&lt;/a&gt;
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsUrchinTrackingModule   {

  private String id;
  private String campaign;
  private String source;
  private String medium;
  private String term;
  private String content;
  private URI referrer;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsUrchinTrackingModule () {
  }

  /**
  * @return id The Campaign ID is used to identify a specific ad campaign or promotion This is a required key for GA4 data import.
  */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
  * @param id The Campaign ID is used to identify a specific ad campaign or promotion This is a required key for GA4 data import.
  */
  @SuppressWarnings("unused")
  public void setId(String id) {
    this.id = id;
  }

  /**
  * @return campaign Product, slogan, promo code
  */
  @JsonProperty("campaign")
  public String getCampaign() {
    return campaign;
  }

  /**
  * @param campaign Product, slogan, promo code
  */
  @SuppressWarnings("unused")
  public void setCampaign(String campaign) {
    this.campaign = campaign;
  }

  /**
  * @return source Referrer. Identifies which site sent the traffic, and is a required parameter Example: the referrer (e.g. google, newsletter)
  */
  @JsonProperty("source")
  public String getSource() {
    return source;
  }

  /**
  * @param source Referrer. Identifies which site sent the traffic, and is a required parameter Example: the referrer (e.g. google, newsletter)
  */
  @SuppressWarnings("unused")
  public void setSource(String source) {
    this.source = source;
  }

  /**
  * @return medium Marketing medium (e.g. cpc, banner, email) Identifies what type of link was used, such as cost per click or email.
  */
  @JsonProperty("medium")
  public String getMedium() {
    return medium;
  }

  /**
  * @param medium Marketing medium (e.g. cpc, banner, email) Identifies what type of link was used, such as cost per click or email.
  */
  @SuppressWarnings("unused")
  public void setMedium(String medium) {
    this.medium = medium;
  }

  /**
  * @return term Identifies search terms, the paid keywords
  */
  @JsonProperty("term")
  public String getTerm() {
    return term;
  }

  /**
  * @param term Identifies search terms, the paid keywords
  */
  @SuppressWarnings("unused")
  public void setTerm(String term) {
    this.term = term;
  }

  /**
  * @return content Identifies what specifically was clicked to bring the user to the site, such as a banner ad or a text link. It is often used for A/B testing and content-targeted ads
  */
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  /**
  * @param content Identifies what specifically was clicked to bring the user to the site, such as a banner ad or a text link. It is often used for A/B testing and content-targeted ads
  */
  @SuppressWarnings("unused")
  public void setContent(String content) {
    this.content = content;
  }

  /**
  * @return referrer Identifies the referrer URL as it's not passed by default by browser HTTP request and therefore not available in the `document.referrer` attribute Google Analytics will use \"utm_referrer\" over \"document.referrer\" set in AnalyticsBrowser utm_source is also a referer but in a named format
  */
  @JsonProperty("referrer")
  public URI getReferrer() {
    return referrer;
  }

  /**
  * @param referrer Identifies the referrer URL as it's not passed by default by browser HTTP request and therefore not available in the `document.referrer` attribute Google Analytics will use \"utm_referrer\" over \"document.referrer\" set in AnalyticsBrowser utm_source is also a referer but in a named format
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
    AnalyticsUrchinTrackingModule analyticsUrchinTrackingModule = (AnalyticsUrchinTrackingModule) o;
    return Objects.equals(id, analyticsUrchinTrackingModule.id) &&
        Objects.equals(campaign, analyticsUrchinTrackingModule.campaign) &&
        Objects.equals(source, analyticsUrchinTrackingModule.source) &&
        Objects.equals(medium, analyticsUrchinTrackingModule.medium) &&
        Objects.equals(term, analyticsUrchinTrackingModule.term) &&
        Objects.equals(content, analyticsUrchinTrackingModule.content) &&
        Objects.equals(referrer, analyticsUrchinTrackingModule.referrer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, campaign, source, medium, term, content, referrer);
  }

  @Override
  public String toString() {
    return "class AnalyticsUrchinTrackingModule {\n" +
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
