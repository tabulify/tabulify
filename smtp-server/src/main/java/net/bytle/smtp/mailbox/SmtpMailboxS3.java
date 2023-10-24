package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.email.BMailMimeMessage;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.s3.AwsBucket;
import net.bytle.s3.AwsObject;
import net.bytle.smtp.SmtpUser;
import net.bytle.smtp.filter.DmarcFilter;
import net.bytle.vertx.ConfigAccessor;

public class SmtpMailboxS3 extends SmtpMailbox {

  private final AwsBucket awsBucket;

  public SmtpMailboxS3(SmtpUser smtpUser, Vertx vertx, ConfigAccessor configAccessor) throws IllegalConfiguration {
    super(smtpUser, vertx, configAccessor);
    this.awsBucket = AwsBucket.init(vertx, configAccessor);
  }

  @Override
  public Future<Void> deliver(BMailMimeMessage mimeMessage) {

    AwsObject awsObject;

    if (this.getSmtpUser().getName().equals("dmarc")) {

      awsObject = DmarcFilter.parse(mimeMessage);

    } else {
      String messageId = mimeMessage.getMessageId();

      awsObject = AwsObject.create(messageId + ".eml")
        .setContent(mimeMessage.getPlainText());
    }

    return this.awsBucket.putObject(awsObject);

  }

}
