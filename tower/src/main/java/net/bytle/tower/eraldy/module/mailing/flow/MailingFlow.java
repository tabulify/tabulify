package net.bytle.tower.eraldy.module.mailing.flow;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.manual.EmailAstDocumentBuilder;
import net.bytle.tower.eraldy.model.manual.EmailTemplateVariables;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingInputProps;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingJobInputProps;
import net.bytle.tower.eraldy.module.mailing.model.*;
import net.bytle.tower.util.RichSlateAST;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.TowerSmtpClientService;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.WebFlowAbs;

import java.util.Arrays;

public class MailingFlow extends WebFlowAbs {


  public MailingFlow(EraldyApiApp towerApp) {
    super(towerApp);

  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  @Override
  public FlowType getFlowType() {
    return FlowType.MAILING;
  }

  public Future<MailingJob> execute(Mailing mailing) {

    MailingStatus status = mailing.getStatus();

    if (Arrays.asList(MailingStatus.COMPLETED, MailingStatus.PAUSED, MailingStatus.CANCELED).contains(status)) {
      return Future.failedFuture(TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.BAD_STATE_400)
        .setMessage("The mailing (" + mailing + ") can not be executed because the status is " + status)
        .build()
      );
    }

    Future<Void> createRequest = Future.succeededFuture();
    if (status == MailingStatus.OPEN) {
      createRequest = this.getApp().getMailingProvider().createRequest(mailing);
    }

    return createRequest
      .compose(v -> this.createJob(mailing))
      .compose(this::executeRows)
      .compose(Future::succeededFuture);


  }

  private Future<MailingJob> executeRows(MailingJob mailingJob) {

    return this.getApp()
      .getMailingItemProvider()
      .getItemsForJobExecution(mailingJob)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          return this.closeJobAndMailing(mailingJob, "No rows to process anymore");
        }

        return Future.succeededFuture(mailingJob);
      });

  }

  private Future<MailingJob> closeJobAndMailing(MailingJob mailingJob, String statusMessage) {
    return this.closeJob(mailingJob, statusMessage, true);
  }

  private Future<MailingJob> closeJob(MailingJob mailingJob, String statusMessage, boolean closeMailing) {


    return this.getApp()
      .getHttpServer()
      .getServer()
      .getPostgresClient()
      .getPool()
      .withTransaction(connection -> {
        /**
         * Update Mailing Job
         */
        MailingJobInputProps mailingJobInputProps = new MailingJobInputProps();
        mailingJobInputProps.setStatusMessage(statusMessage);
        mailingJobInputProps.setStatus(MailingJobStatus.COMPLETED);
        return this.getApp()
          .getMailingJobProvider()
          .updateMailingJob(connection, mailingJob, mailingJobInputProps)
          .compose(v -> {
            if (!closeMailing) {
              return Future.succeededFuture(mailingJob);
            }
            MailingInputProps mailingInputProps = new MailingInputProps();
            mailingInputProps.setStatus(MailingStatus.COMPLETED);
            return Future.failedFuture(new InternalException("Close Mailing not yet implemented"));
          });
      });

  }

  private Future<MailingJob> createJob(Mailing mailing) {


    return this.getApp()
      .getMailingJobProvider()
      .insertMailingJob(mailing);


  }


  /**
   * @return the maximum number of failure on a row
   */
  public int getMaxCountFailureOnRow() {
    return 2;
  }

  /**
   * Deliver, send and item
   * (The mailing item has been build with a user with an address and a given name)
   */
  public Future<MailingItem> deliverItem(MailingItem mailingItem) {
    if (mailingItem.getStatus() == MailingItemStatus.OK) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_STATE_400)
        .setMessage("The item (" + mailingItem + ") has already been delivered")
        .build()
      );
    }
    User recipientUser = mailingItem.getListUser().getUser();
    return this.sendMail(recipientUser, mailingItem.getMailing())
      .compose(
        mailingResult -> {

          mailingItem.setStatus(MailingItemStatus.OK);
          // mailingResult.getMessageID();
          return Future.succeededFuture(mailingItem);

        },
        err -> {
          mailingItem.setStatus(MailingItemStatus.ERROR);
          mailingItem.setStatusMessage(err.getMessage());
          return Future.succeededFuture(mailingItem);

        });
  }

  /**
   * A function to send a test email and a mailing email
   * @param recipient - the recipient
   * @param mailing - the mailing
   * @return the mail result
   */
  public Future<MailResult> sendMail(User recipient, Mailing mailing) {

    TowerSmtpClientService smtpClientService = this.getApp().getEmailSmtpClientService();

    return this.getApp().getMailingProvider().buildEmailAuthorAtRequestTimeEventually(mailing)
      .compose(emailAuthor -> {

        /**
         * Author
         */
        String authorEmailAsString = emailAuthor.getEmailAddress();
        EmailAddress authorEmailAddress;
        try {
          authorEmailAddress = new EmailAddress(authorEmailAsString);
        } catch (EmailCastException e) {
          return Future.failedFuture(new InternalException("The email (" + authorEmailAsString + ") of the author is invalid", e));
        }

        /**
         * Recipient
         */
        String inputEmailAddress = recipient.getEmailAddress();
        EmailAddress recipientEmailAddress;
        try {
          recipientEmailAddress = new EmailAddress(inputEmailAddress);
        } catch (EmailCastException e) {
          return Future.failedFuture(new InternalException("The email (" + inputEmailAddress + ") of the recipient is invalid", e));
        }

        /**
         * Variables
         */
        String givenName = recipient.getGivenName();
        if (givenName == null) {
          givenName = recipientEmailAddress.getLocalBox();
        }
        JsonObject variables = EmailTemplateVariables.create()
          .setRecipientGivenName(givenName)
          .getVariables();

        String emailSubjectRsAst = mailing.getEmailSubject();
        String emailSubject = "Test email of the mailing " + mailing.getName();
        if (emailSubjectRsAst != null) {
          emailSubject = RichSlateAST.createFromFormInputAst(emailSubjectRsAst)
            .addVariables(variables)
            .build()
            .toEmailText();
        }
        MailMessage email = smtpClientService.createVertxMailMessage()
          .setTo(recipientEmailAddress.toNormalizedString())
          .setFrom(authorEmailAddress.toNormalizedString())
          .setSubject(emailSubject);

        String mailBody = mailing.getEmailBody();

        if (mailBody != null) {

          String emailPreview = mailing.getEmailPreview();
          if (emailPreview != null) {
            emailPreview = RichSlateAST.createFromFormInputAst(emailPreview)
              .addVariables(variables)
              .build()
              .toEmailText();
          }

          /**
           * HTML Body Building
           */
          RichSlateAST richSlateAST = new RichSlateAST
            .Builder()
            .addVariables(variables)
            .setDocument(
              EmailAstDocumentBuilder.create()
                .setTitle(emailSubject)
                .setLanguage(mailing.getEmailLanguage())
                .setPreview(emailPreview)
                .setBody(new JsonArray(mailing.getEmailBody()))
                .build()
            )
            .build();
          email.setHtml(richSlateAST.toEmailHTML());
          email.setText(richSlateAST.toEmailText());

        }

        return smtpClientService
          .getVertxMailClientForSenderWithSigning(recipientEmailAddress.getDomainName().toStringWithoutRoot())
          .sendMail(email);
      });
  }

}
