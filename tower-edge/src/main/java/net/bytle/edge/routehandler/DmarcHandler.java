package net.bytle.edge.routehandler;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.dmarc.DmarcFeedback;
import net.bytle.dmarc.DmarcFeedbackMetadata;
import net.bytle.dmarc.DmarcManager;
import net.bytle.dmarc.DmarcReport;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.s3.AwsBucket;
import net.bytle.s3.AwsObject;
import net.bytle.type.MediaTypes;
import net.bytle.type.time.Timestamp;

import java.time.LocalDateTime;

/**
 * Dmarc Post Handler
 * from a <a href="https://forwardemail.net/en/faq#do-you-support-webhooks">forwardEmail json data</a>
 */
public class DmarcHandler implements Handler<RoutingContext> {


  /**
   * The {@link LocalDateTime#toString()} print a short iso string
   * where the seconds and minute may be missing.
   * To have a consistent number of string, we use a fix pattern
   */
  private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
  private static final String ERROR_DIRECTORY = "error/";
  public static final String DMARC_PATH = "/dmarc";

  public static Handler<RoutingContext> create() {
    return new DmarcHandler();
  }

  public static Future<Void> parseAndUpload(JsonObject json) {

    AwsObject awsObject;
    try {
      DmarcReport dmarcReport = DmarcManager.getDmarcReportFromEmail(json);
      String remoteUniquePath = getRemoteUniquePath(dmarcReport.getFeedbackObject());
      awsObject = AwsObject.create(remoteUniquePath)
        .setContent(dmarcReport.getFeedbackXml())
        .setMediaType(MediaTypes.TEXT_XML);
    } catch (IllegalStructure e) {
      JsonObject errorJsonWithDataObject = new JsonObject();
      errorJsonWithDataObject.put("data", json);
      JsonObject errorJson = new JsonObject();
      errorJson.put("message", e.getMessage());
      errorJson.put("code", e.getClass().getName());
      errorJsonWithDataObject.put("error", errorJson);
      Timestamp now = Timestamp.createFromNow();
      String errorName = now.toString(ISO_DATE_FORMAT);
      String partition = now.toString("yyyy-MM");
      awsObject = AwsObject.create(ERROR_DIRECTORY + partition + "/" + errorName + "-error.json")
        .setContent(errorJsonWithDataObject.encode())
        .setMediaType(MediaTypes.TEXT_JSON);
    }

    AwsBucket awsBucket = AwsBucket.get();
    if (awsBucket == null) {
      throw new InternalException("awsS3 object was not initialized.");
    }
    return awsBucket.putObject(awsObject);

  }

  /**
   * @return a file path with a name that is unique and a parent path to create a partition on the remote bucket
   */
  public static String getRemoteUniquePath(DmarcFeedback dmarcFeedback) {


    DmarcFeedbackMetadata reportMetadata = dmarcFeedback.getReportMetadata();
    Timestamp beginTimestamp = Timestamp.createFromEpochSec(reportMetadata.getReportDateRange().getBegin());
    String monthPartition = beginTimestamp.toString("YYYY-MM");
    String yearPartition = beginTimestamp.toString("YYYY");
    String beginIsoString = beginTimestamp.toString(ISO_DATE_FORMAT);
    String endIsoString = Timestamp.createFromEpochSec(reportMetadata.getReportDateRange().getEnd()).toString(ISO_DATE_FORMAT);
    return yearPartition + "/" + monthPartition + "/"
      + beginIsoString + "!" +
      endIsoString + "!" +
      reportMetadata.getOrganisationName().trim() + "!" +
      dmarcFeedback.getPolicyPublished().getDomain().trim() +
      ".xml";

  }

  @Override
  public void handle(RoutingContext routingContext) {

    JsonObject json = routingContext.body().asJsonObject();
    parseAndUpload(json)
      .onFailure(routingContext::fail)
      .onSuccess(Void-> routingContext
        .response()
        .end());

  }


}
