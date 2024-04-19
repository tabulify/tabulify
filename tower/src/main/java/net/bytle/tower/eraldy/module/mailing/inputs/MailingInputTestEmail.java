package net.bytle.tower.eraldy.module.mailing.inputs;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.type.EmailAddress;

/**
 * Mailing Input Props
 * for the creation and or modification of a mailing
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailingInputTestEmail {



  private EmailAddress recipientEmailAddress;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public MailingInputTestEmail() {
  }

  @JsonProperty("recipientEmailAddress")
  public EmailAddress getRecipientEmailAddress() {
    return recipientEmailAddress;
  }


  @SuppressWarnings("unused")
  public void setRecipientEmailAddress(EmailAddress recipientEmailAddress) {
    this.recipientEmailAddress = recipientEmailAddress;
  }


}
