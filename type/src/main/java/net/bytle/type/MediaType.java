package net.bytle.type;


/**
 * The media type defines the content type:
 * * for a file system (web): mime type (ie csv, ...)
 * * for a memory system: list / queue / gen
 * * for a relational system: table, view, query
 *
 * This is an interface to be able to create enum
 *
 * When checking for equality, you need to check the equality on the `toString`
 * method if the enum class are not the same
 *
 * <p>
 * The name was taken from the term `Internet media type`
 * from the mime specification.
 * <a href="http://www.iana.org/assignments/media-types/media-types.xhtml">MediaType</a>
 */
public interface MediaType {

  public static final String TEXT_TYPE = "text";

  /**
   *
   * @return the format (plain, jpeg, mpeg, ...)
   */
  String getSubType();

  /**
   *
   * @return the top-level type (text, image, video, audio, application)
   */
  String getType();

  /**
   *
   * @return true if this is a container of object (directory, schema, catalog, ...)
   */
  boolean isContainer();

  /**
   *
   * @return the file extension if any
   */
  String getExtension();


  default Boolean isText() {

    return getType().equals(TEXT_TYPE);

  }


}
