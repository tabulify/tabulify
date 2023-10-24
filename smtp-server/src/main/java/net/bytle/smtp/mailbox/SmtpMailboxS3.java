package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.s3.AwsBucket;
import net.bytle.s3.AwsObject;
import net.bytle.smtp.SmtpMessage;
import net.bytle.smtp.SmtpUser;
import net.bytle.smtp.milter.SmtpMilter;
import net.bytle.vertx.ConfigAccessor;

import java.util.List;

public class SmtpMailboxS3 extends SmtpMailbox {

  private final AwsBucket awsBucket;

  public SmtpMailboxS3(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) throws IllegalConfiguration {
    super(smtpUser, vertx, milters, configAccessor);
    this.awsBucket = AwsBucket.init(vertx, configAccessor);
  }


  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {


    AwsObject awsObject = AwsObject
      .create(smtpMessage.getPath())
      .setContent(smtpMessage.getBytes())
      .setMediaType(smtpMessage.getMediaType());
    return this.awsBucket.putObject(awsObject);

  }

}
