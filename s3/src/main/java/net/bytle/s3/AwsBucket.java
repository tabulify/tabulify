package net.bytle.s3;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ServerStartLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.concurrent.CompletableFuture;

public class AwsBucket {

  private static final Logger LOGGER = LogManager.getLogger();
  static final String S3_BUCKET_NAME = "s3_bucket_name";
  private static AwsBucket awsBucket;

  private final String bucketName;
  private final AwsClient awsClient;

  public AwsBucket(AwsClient awsClient, String bucketName) {

    this.bucketName = bucketName;
    if (this.bucketName == null) {
      throw new IllegalArgumentException("The s3 bucket name end is mandatory");
    }
    this.awsClient = awsClient;


  }

  public Future<AwsBucket> checkConnection() {

    ListObjectsV2Request listObjects = ListObjectsV2Request
      .builder()
      .bucket(bucketName)
      .maxKeys(1)
      .build();

    CompletableFuture<ListObjectsV2Response> listObjectsFuture = awsClient.getS3Client().listObjectsV2(listObjects);
    Context context = awsClient.getVertx().getOrCreateContext();
    // https://vertx.io/docs/vertx-core/java/#_completionstage_interoperability
    return Future.fromCompletionStage(listObjectsFuture.toCompletableFuture(), context)
      .compose(listObjectsV2Response -> {
        ServerStartLogger.START_LOGGER.info("Connection to the bucket (" + this + ") successful");
        return Future.succeededFuture(this);
      }, err -> {
        throw new IllegalArgumentException("Unable to create a connection to the bucket (" + this + ")", err);
      });


  }

  public static AwsBucket get() {
    return awsBucket;
  }

  public static AwsBucket init(Vertx vertx, ConfigAccessor configAccessor) throws IllegalConfiguration {

    AwsClient awsClient = AwsClient.create(vertx, configAccessor);
    String bucketName = configAccessor.getString(AwsBucket.S3_BUCKET_NAME);
    if (bucketName == null) {
      throw new IllegalConfiguration("The s3 bucket name end point was not found via the configuration (" + AwsBucket.S3_BUCKET_NAME + ")");
    }
    return createAwsBucket(bucketName,awsClient);

  }

  private static AwsBucket createAwsBucket(String bucketName, AwsClient awsClient) {
    awsBucket = new AwsBucket(awsClient, bucketName);
    return awsBucket;

  }

  public Future<Void> putObject(AwsObject awsObject) {

    PutObjectRequest putOb = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(awsObject.getObjectPath())
      .contentType(awsObject.getContentType().toString())
      .metadata(awsObject.getMetadataAsHeaderMap())
      .build();

    AsyncRequestBody requestBody = awsObject.getRequestBody();
    CompletableFuture<PutObjectResponse> completableFuture = awsClient.getS3Client().putObject(putOb, requestBody);
    Context context = awsClient.getVertx().getOrCreateContext();
    // https://vertx.io/docs/vertx-core/java/#_completionstage_interoperability
    return Future.fromCompletionStage(completableFuture.toCompletableFuture(), context)
      .onFailure(event -> LOGGER.error("Unable to create a connection to bucket (" + this + ")"))
      .compose(listObjectsV2Response -> {
        LOGGER.info("Successfully placed " + awsObject.getObjectPath() + " into bucket " + bucketName);
        return Future.succeededFuture();
      });


  }

  @Override
  public String toString() {
    return bucketName+"@"+awsClient;
  }
}
