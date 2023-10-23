package net.bytle.smtp.mailbox;

import io.vertx.core.Vertx;
import jakarta.mail.MessagingException;
import net.bytle.email.BMailMimeMessage;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.s3.AwsBucket;
import net.bytle.s3.AwsObject;
import net.bytle.smtp.SmtpException;
import net.bytle.smtp.SmtpReplyCode;
import net.bytle.smtp.SmtpUser;
import net.bytle.smtp.filter.DmarcFilter;
import net.bytle.vertx.ConfigAccessor;

public class SmtpMailboxS3 extends SmtpMailbox {


  private final AwsBucket awsBucket;

  public SmtpMailboxS3(Vertx vertx, ConfigAccessor configAccessor) throws IllegalConfiguration {
    this.awsBucket = AwsBucket.init(vertx, configAccessor);
  }

  @Override
  public void deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage) throws SmtpException {

    AwsObject awsObject;

    if (smtpUser.getName().equals("dmarc")) {

      awsObject = DmarcFilter.parse(mimeMessage);

    } else {
      String messageId;
      try {
        messageId = mimeMessage.getMessageId();
      } catch (MessagingException e) {
        throw new SmtpException("Unable to get the message id", e, SmtpReplyCode.SYNTAX_ERROR_501);
      }
      awsObject = AwsObject.create(messageId + ".eml")
        .setContent(mimeMessage.getPlainText());
    }

    this.awsBucket.putObject(awsObject);

  }

  @Override
  public String getName() {
    return "s3";
  }

}
