package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Email Campaign creation and/or modification
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignEmailPostBody   {


  protected String campaignGuid;

  protected String campaignHandle;

  protected String emailSubject;

  protected String listGuid;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public CampaignEmailPostBody () {
  }

  /**
  * @return campaignGuid The service global id, if you want to update the identifier
  */
  @JsonProperty("campaignGuid")
  public String getCampaignGuid() {
    return campaignGuid;
  }

  /**
  * @param campaignGuid The service global id, if you want to update the identifier
  */
  @SuppressWarnings("unused")
  public void setCampaignGuid(String campaignGuid) {
    this.campaignGuid = campaignGuid;
  }

  /**
  * @return campaignHandle The campaign handle (unique code)
  */
  @JsonProperty("campaignHandle")
  public String getCampaignHandle() {
    return campaignHandle;
  }

  /**
  * @param campaignHandle The campaign handle (unique code)
  */
  @SuppressWarnings("unused")
  public void setCampaignHandle(String campaignHandle) {
    this.campaignHandle = campaignHandle;
  }

  /**
  * @return emailSubject The email subject
  */
  @JsonProperty("emailSubject")
  public String getEmailSubject() {
    return emailSubject;
  }

  /**
  * @param emailSubject The email subject
  */
  @SuppressWarnings("unused")
  public void setEmailSubject(String emailSubject) {
    this.emailSubject = emailSubject;
  }

  /**
  * @return listGuid The list audience (the valid subscribers to the list)
  */
  @JsonProperty("listGuid")
  public String getListGuid() {
    return listGuid;
  }

  /**
  * @param listGuid The list audience (the valid subscribers to the list)
  */
  @SuppressWarnings("unused")
  public void setListGuid(String listGuid) {
    this.listGuid = listGuid;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CampaignEmailPostBody campaignEmailPostBody = (CampaignEmailPostBody) o;
    return Objects.equals(campaignGuid, campaignEmailPostBody.campaignGuid) &&
        Objects.equals(campaignHandle, campaignEmailPostBody.campaignHandle) &&
        Objects.equals(emailSubject, campaignEmailPostBody.emailSubject) &&
        Objects.equals(listGuid, campaignEmailPostBody.listGuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(campaignGuid, campaignHandle, emailSubject, listGuid);
  }

  @Override
  public String toString() {
    return "class CampaignEmailPostBody {\n" +
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
