package net.bytle.smtp;

import io.vertx.core.buffer.Buffer;
import jakarta.mail.MessagingException;
import net.bytle.email.BMailMimeMessage;
import net.bytle.java.JavaEnvs;
import net.bytle.type.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simplejavamail.converter.EmailConverter;

import java.io.IOException;

public class SmtpReception {


  private static final Logger LOGGER = LogManager.getLogger(SmtpReception.class);
  private final SmtpTransactionState transactionState;

  public SmtpReception(SmtpTransactionState transactionState) {

    this.transactionState = transactionState;

  }


  /**
   * A reception is when you store the SMTP transaction.
   * The {@link SmtpDelivery#delivery(SmtpEnvelope)} is normally done later on schedule
   */
  public SmtpEnvelope reception() throws SmtpException {

    BMailMimeMessage mimeMessage;
    try {

      /**
       * Message size check
       */
      Buffer textMessage = transactionState.getMessage();
      Integer messageSizeFromMailCommand = transactionState.getMessageSizeFromMailCommand();
      if (messageSizeFromMailCommand != null) {
        int messageSize = textMessage.length();
        if (messageSize != messageSizeFromMailCommand) {
          throw SmtpException.create(SmtpReplyCode.ERROR_IN_PROCESSING_451, "The message size received (" + messageSize + ") is different that the MAIL command size (" + messageSizeFromMailCommand + ")");
        }
      }

      String eml = textMessage.toString();
      if (JavaEnvs.IS_DEV) {
        System.out.println(eml);
      }
      mimeMessage = BMailMimeMessage.createFromMimeMessage(EmailConverter.emlToMimeMessage(eml));
      LOGGER.trace("Message received: Message (" + Strings.createFromString(mimeMessage.getSubject()).toMaxLength(20, "...") + ") from (" + mimeMessage.getFromAsString() + ")");

    } catch (MessagingException | IOException e) {
      throw SmtpException.createForInternalException("Reception Error: ", e);
    }

    /**
     * Mail From Address Handling
     * <p>
     * During the transaction, the {@link SmtpFiltering}
     * checks have already run
     * * on Ip of the remote
     * * and Domain of the `From` email
     * * {@link SmtpSpf}
     */

    /**
     * Email Dkim
     */
    SmtpSession session = this.transactionState.getSession();
    SmtpDkim.check(session);

    /**
     * Add trace header upon reception
     */
    SmtpReceptionTracing.addTraceHeader(session, mimeMessage);

    /**
     * Create the envelope
     */
    return SmtpEnvelope.create(
      transactionState.getSender(),
      transactionState.getRecipients(),
      mimeMessage
    );

  }


  public static SmtpReception create(SmtpTransactionState transactionState) throws SmtpException {
    return new SmtpReception(transactionState);
  }


  public SmtpSession getSession() {
    return this.transactionState.getSession();
  }
}
