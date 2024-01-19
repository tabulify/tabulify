package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * The data that permits to derive the channel (ie how the user came in). Mostly UTM (Urchin Tracking Module) data with the document referer.     See:     * &lt;a href&#x3D;\&quot;https://datacadamia.com/marketing/analytics/utm\&quot;&gt;UTM&lt;/a&gt;     * &lt;a href&#x3D;\&quot;https://ga-dev-tools.google/campaign-url-builder/\&quot;&gt;UTM builder&lt;/a&gt;     * &lt;a href&#x3D;\&quot;https://support.google.com/analytics/answer/10917952\&quot;&gt;[GA4] URL builders: Collect campaign data with custom URLs&lt;/a&gt;
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventChannel   {


  protected String utmCampaignId;

  protected String utmCampaignName;

  protected String utmSource;

  protected String utmMedium;

  protected String utmTerm;

  protected String utmContent;

  protected URI utmReferrer;

  protected URI documentReferrer;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventChannel () {
  }

  /**
  * @return utmCampaignId The Campaign ID is used to identify a specific ad campaign or promotion Known as utm_id, this is a required key for GA4 data import.
  */
  @JsonProperty("utmCampaignId")
  public String getUtmCampaignId() {
    return utmCampaignId;
  }

  /**
  * @param utmCampaignId The Campaign ID is used to identify a specific ad campaign or promotion Known as utm_id, this is a required key for GA4 data import.
  */
  @SuppressWarnings("unused")
  public void setUtmCampaignId(String utmCampaignId) {
    this.utmCampaignId = utmCampaignId;
  }

  /**
  * @return utmCampaignName Product, slogan, promo code
  */
  @JsonProperty("utmCampaignName")
  public String getUtmCampaignName() {
    return utmCampaignName;
  }

  /**
  * @param utmCampaignName Product, slogan, promo code
  */
  @SuppressWarnings("unused")
  public void setUtmCampaignName(String utmCampaignName) {
    this.utmCampaignName = utmCampaignName;
  }

  /**
  * @return utmSource Referrer. Identifies which site sent the traffic, and is a required parameter Example: the referrer (e.g. google, newsletter)
  */
  @JsonProperty("utmSource")
  public String getUtmSource() {
    return utmSource;
  }

  /**
  * @param utmSource Referrer. Identifies which site sent the traffic, and is a required parameter Example: the referrer (e.g. google, newsletter)
  */
  @SuppressWarnings("unused")
  public void setUtmSource(String utmSource) {
    this.utmSource = utmSource;
  }

  /**
  * @return utmMedium Marketing medium (e.g. cpc, banner, email) Identifies what type of link was used, such as cost per click or email.
  */
  @JsonProperty("utmMedium")
  public String getUtmMedium() {
    return utmMedium;
  }

  /**
  * @param utmMedium Marketing medium (e.g. cpc, banner, email) Identifies what type of link was used, such as cost per click or email.
  */
  @SuppressWarnings("unused")
  public void setUtmMedium(String utmMedium) {
    this.utmMedium = utmMedium;
  }

  /**
  * @return utmTerm Identifies search terms, the paid keywords
  */
  @JsonProperty("utmTerm")
  public String getUtmTerm() {
    return utmTerm;
  }

  /**
  * @param utmTerm Identifies search terms, the paid keywords
  */
  @SuppressWarnings("unused")
  public void setUtmTerm(String utmTerm) {
    this.utmTerm = utmTerm;
  }

  /**
  * @return utmContent Identifies what specifically was clicked to bring the user to the site, such as a banner ad or a text link. It is often used for A/B testing and content-targeted ads
  */
  @JsonProperty("utmContent")
  public String getUtmContent() {
    return utmContent;
  }

  /**
  * @param utmContent Identifies what specifically was clicked to bring the user to the site, such as a banner ad or a text link. It is often used for A/B testing and content-targeted ads
  */
  @SuppressWarnings("unused")
  public void setUtmContent(String utmContent) {
    this.utmContent = utmContent;
  }

  /**
  * @return utmReferrer Identifies the referrer URL as it's not passed by default by browser HTTP request and therefore not available in the `document.referrer` attribute Google Analytics will use \"utm_referrer\" over \"document.referrer\" set in AnalyticsBrowser utm_source is also a referer but in a named format
  */
  @JsonProperty("utmReferrer")
  public URI getUtmReferrer() {
    return utmReferrer;
  }

  /**
  * @param utmReferrer Identifies the referrer URL as it's not passed by default by browser HTTP request and therefore not available in the `document.referrer` attribute Google Analytics will use \"utm_referrer\" over \"document.referrer\" set in AnalyticsBrowser utm_source is also a referer but in a named format
  */
  @SuppressWarnings("unused")
  public void setUtmReferrer(URI utmReferrer) {
    this.utmReferrer = utmReferrer;
  }

  /**
  * @return documentReferrer The `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @JsonProperty("documentReferrer")
  public URI getDocumentReferrer() {
    return documentReferrer;
  }

  /**
  * @param documentReferrer The `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @SuppressWarnings("unused")
  public void setDocumentReferrer(URI documentReferrer) {
    this.documentReferrer = documentReferrer;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventChannel analyticsEventChannel = (AnalyticsEventChannel) o;
    return

            Objects.equals(utmCampaignId, analyticsEventChannel.utmCampaignId) && Objects.equals(utmCampaignName, analyticsEventChannel.utmCampaignName) && Objects.equals(utmSource, analyticsEventChannel.utmSource) && Objects.equals(utmMedium, analyticsEventChannel.utmMedium) && Objects.equals(utmTerm, analyticsEventChannel.utmTerm) && Objects.equals(utmContent, analyticsEventChannel.utmContent) && Objects.equals(utmReferrer, analyticsEventChannel.utmReferrer) && Objects.equals(documentReferrer, analyticsEventChannel.documentReferrer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(utmCampaignId, utmCampaignName, utmSource, utmMedium, utmTerm, utmContent, utmReferrer, documentReferrer);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
