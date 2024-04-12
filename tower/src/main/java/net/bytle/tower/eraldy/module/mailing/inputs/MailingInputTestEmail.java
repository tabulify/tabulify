package net.bytle.tower.eraldy.module.mailing.inputs;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mailing Input Props
 * for the creation and or modification of a mailing
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailingInputTestEmail {



  private String recipientEmailAddress;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public MailingInputTestEmail() {
  }

  @JsonProperty("recipientEmailAddress")
  public String getRecipientEmailAddress() {
    return recipientEmailAddress;
  }


  @SuppressWarnings("unused")
  public void setRecipientEmailAddress(String recipientEmailAddress) {
    this.recipientEmailAddress = recipientEmailAddress;
  }


}
