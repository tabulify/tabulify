package com.tabulify.type;


/**
 * The media type defines the content type:
 * * for a file system (web): mime type (ie csv, ...)
 * * for a memory system: list / queue / gen
 * * for a relational system: table, view, query
 * <p></p>
 * This is an interface to be able to create enum
 * <p></p>
 * When checking for equality, you need to check the equality on the `toString`
 * method if the enum class are not the same
 *
 * <p>
 * The name was taken from the term `Internet media type`
 * from the mime specification.
 * <a href="http://www.iana.org/assignments/media-types/media-types.xhtml">MediaType</a>
 */
public interface MediaType {

  String TEXT_TYPE = "text";

  /**
   * @return the format (plain, jpeg, mpeg, ...)
   * This is generally also the file extension
   */
  String getSubType();

  /**
   * @return the top-level type (text, image, video, audio, application)
   * This is a file type category.
   * May be empty if the string has no separator
   * (empty and not null to avoid null exception on equality (ie `type.equals(type)`)
   * Example: if the string is `json`, by default, type is empty while `text/json` is not.
   */
  String getType();

  /**
   * @return true if this is a container of object (directory, schema, catalog, ...)
   * ie an object without any content
   */
  boolean isContainer();

  /**
   * @return true if this media type is a runtime resource
   * A runtime resource is created at runtime with an executable
   * Why? We need it because a runtime resource may be created from a string
   * and not from a path
   * Example: a sql request created from a sql string
   */
  default boolean isRuntime(){
    return false;
  }

  /**
   * @return the file extension (by default the {@link #getSubType() subtype}
   */
  String getExtension();


  /**
   * Utility
   *
   * @return true if this media type is a text file
   */
  default Boolean isText() {

    String lowerType = toString().toLowerCase();
    // Common text types
    return lowerType.startsWith(TEXT_TYPE + "/") ||
      lowerType.equals("application/x-sh") ||
      lowerType.equals("application/x-shellscript") ||
      lowerType.equals("application/x-python") ||
      lowerType.equals("application/x-perl") ||
      lowerType.equals("application/x-ruby") ||
      lowerType.equals("application/javascript") ||
      lowerType.equals("application/json") ||
      lowerType.equals("application/xml") ||
      lowerType.equals("application/x-httpd-php");
  }

  /**
   * @return a kind, a single name identifier
   */
  default KeyNormalizer getKind() {
    return KeyNormalizer.createSafe(getSubType());
  }

}
