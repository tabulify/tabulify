package net.bytle.s3;

import io.vertx.core.Vertx;
import net.bytle.vertx.ConfigAccessor;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

public class AwsClient {

  /**
   * This object is a long live object,
   * but it has also a close method
   */
  private final S3AsyncClient s3;

  private static final String S3_ACCESS_KEY_ID = "s3.access.key.id";
  /**
   * example: ca-mtl
   */
  private static final String S3_REGION = "s3.region";
  private static final String S3_ACCESS_KEY_SECRET = "s3.access.key.secret";
  private static final String S3_URL_ENDPOINT = "s3.url.endpoint";
  private final Region region;
  private final URI endpoint;
  private final Vertx vertx;


  private AwsClient(Vertx vertx, ConfigAccessor configAccessor) {

    this.vertx = vertx;

    AwsCredentials defaultAwsCredential = null;
    try (DefaultCredentialsProvider defaultCredentialsProvider = DefaultCredentialsProvider.create()) {
      defaultAwsCredential = defaultCredentialsProvider.resolveCredentials();
    } catch (SdkClientException e) {
      // Exception when not found
      // Example
      // software.amazon.awssdk.core.exception.SdkClientException:
      // Unable to load credentials from any of the providers in the chain
      // AwsCredentialsProviderChain(credentialsProviders=[SystemPropertyCredentialsProvider()
    }

    String accessKeyId = configAccessor.getString(S3_ACCESS_KEY_ID);
    if (accessKeyId == null && defaultAwsCredential != null) {
      accessKeyId = defaultAwsCredential.accessKeyId();
    }
    if (accessKeyId == null) {
      throw new IllegalArgumentException("The s3 access key id was not found  in the default aws credentials file or with the configuration (" + configAccessor.getPossibleVariableNames(S3_ACCESS_KEY_ID) + ")");
    }
    String accessKeySecret = configAccessor.getString(S3_ACCESS_KEY_SECRET);
    if (accessKeySecret == null && defaultAwsCredential != null) {
      accessKeySecret = defaultAwsCredential.secretAccessKey();
    }
    if (accessKeySecret == null) {
      throw new IllegalArgumentException("The s3 access key secret was not found  in the default aws credentials file or with the configuration (" + configAccessor.getPossibleVariableNames(S3_ACCESS_KEY_SECRET) + ")");
    }

    String finalAccessKeyId = accessKeyId;
    String finalAccessKeySecret = accessKeySecret;
    AwsCredentials credentials = new AwsCredentials() {
      @Override
      public String accessKeyId() {

        return finalAccessKeyId;
      }

      @Override
      public String secretAccessKey() {
        return finalAccessKeySecret;
      }

    };
    AwsCredentialsProvider awsCredentialProvider = StaticCredentialsProvider.create(credentials);


    String url = configAccessor.getString(S3_URL_ENDPOINT);
    if (url == null) {
      throw new IllegalArgumentException("The s3 url end point was not found via the configuration (" + configAccessor.getPossibleVariableNames(S3_URL_ENDPOINT) + ")");
    }

    try {
      this.endpoint = URI.create(url);
    } catch (Exception e) {
      throw new IllegalArgumentException("The s3 url end point (" + url + ") from the configuration (" + configAccessor.getPossibleVariableNames(S3_URL_ENDPOINT) + ") is not valid URL");
    }


    String regionString = configAccessor.getString(S3_REGION);
    if (regionString == null) {
      throw new IllegalArgumentException("The s3 region was not found via the configuration (" + configAccessor.getPossibleVariableNames(S3_REGION) + ")");
    }
    this.region = Region.of(regionString);

    this.s3 = S3AsyncClient
      .builder()
      .region(this.region)
      .endpointOverride(this.endpoint)
      .credentialsProvider(awsCredentialProvider)
      .build();

  }


  /**
   * @param vertx - Vertx is needed to run the s3 HTTP JDK call asynchronously on the vertx loop
   */
  public static AwsClient create(Vertx vertx, ConfigAccessor configAccessor) {
    return new AwsClient(vertx, configAccessor);
  }

  @Override
  public String toString() {
    return region.id() + "@" + endpoint.toString();
  }

  public S3AsyncClient getS3Client() {
    return this.s3;
  }

  public Vertx getVertx() {
    return this.vertx;
  }
}
