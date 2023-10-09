package net.bytle.smtp;

import java.util.List;

public class SmtpDelivery {


  /**
   * 3 choices:
   * * local delivery: storing the email in a local mailbox (disk, http endpoint)
   * * remote delivery: transmitting it to remote mailbox
   * * forwarding it (alias): SRS
   */
  public static void delivery(SmtpEnvelope smtpEnvelope) throws SmtpException {


    /**
     * Recipients
     */
    List<SmtpRecipient> recipientsPaths = smtpEnvelope.getRecipients();
    for (SmtpRecipient recipient : recipientsPaths) {

      try {
        SmtpDeliveryType deliveryType = recipient.getDeliveryType();
        switch (deliveryType) {
          case LOCAL:
            recipient.getLocalUser().getMailBox().deliver(smtpEnvelope);
            break;
          case REMOTE:
            /**
             * Gmail's Bulk Senders Guidelines
             * https://support.google.com/mail/answer/81126
             * DMARC/Postmaster tool
             * https://support.google.com/mail/answer/2451690
             */
            throw SmtpException.createNotSupportedImplemented("Remote delivery is not yet supported");
          default:
            throw SmtpException.createForInternalException("Delivery Type (" + deliveryType + ") was not processed");
        }
      } catch (Exception e) {
        /**
         * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-6.1">...</a>
         * If there is a delivery failure after acceptance of a message, the
         * receiver-SMTP MUST formulate and mail a notification message.  This
         * notification MUST be sent using a null ("<>") reverse-path in the
         * envelope.
         * <p>
         * To not become a backscatterer, we have taken most of the filtering steps
         * before accepting the emails
         * </p>
         * <p>
         * Delivered-To: gerardnico@gmail.com
         * Return-Path: <SRS0=155e=FQ=gmail.com=gerardnico@eraldy.com>
         * X-Original-To: support@combostrap.com
         * X-Complaints-To: abuse@xxxx.net
         * X-Report-Abuse-To: abuse@xxxx.net
         * X-Report-Abuse: abuse@xxx.net
         * X-Eraldy-Sender: rfc822; gerardnico@gmail.com, mail-yw1-f182.google.com, 209.85.128.182
         * X-Eraldy-Version: 10.0.0-alpha
         */
      }
    }




  }
}
