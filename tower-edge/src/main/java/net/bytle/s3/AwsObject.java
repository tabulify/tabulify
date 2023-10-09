package net.bytle.s3;

import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;
import software.amazon.awssdk.core.async.AsyncRequestBody;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AwsObject {
  /**
   * The HTTP header prefix to create a value
   */
  private static final String X_AMZ_META_PREFIX = "x-amz-meta-";
  private final String objectPath;
  private byte[] textContent;
  private MediaType mediaType = MediaTypes.BINARY_FILE;

  Map<String, String> metadata = new HashMap<>();

  public AwsObject(String objectPath) {
    this.objectPath = objectPath;
  }

  public static AwsObject create(String objectPath) {
    return new AwsObject(objectPath);
  }

  public AwsObject setContent(String text) {
    this.textContent = text.getBytes(StandardCharsets.UTF_8);
    return this;
  }

  public AwsObject setMediaType(MediaType mediaTypes) {
    this.mediaType = mediaTypes;
    return this;
  }

  public String getObjectPath() {
    return this.objectPath;
  }

  public MediaType getContentType() {
    return this.mediaType;
  }

  @SuppressWarnings("unused")
  public AwsObject addMetadata(String key, String value) {
    metadata.put(X_AMZ_META_PREFIX + key, value);
    return this;
  }

  public Map<String, String> getMetadataAsHeaderMap() {
    return metadata;
  }

  public AsyncRequestBody getRequestBody() {

    return AsyncRequestBody.fromBytes(this.textContent);

  }

}
