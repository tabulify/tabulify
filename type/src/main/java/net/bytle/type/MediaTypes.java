package net.bytle.type;

import net.bytle.exception.InternalException;
import net.bytle.exception.NotAbsoluteException;
import net.bytle.exception.NullValueException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * A collection of well known MediaType / Mime
 * and of static constructors
 */
public enum MediaTypes implements MediaType {


  BINARY_FILE("application", "octet-stream", false, "bin"),

  // Ubuntu
  // https://stackoverflow.com/questions/18869772/mime-type-for-a-directory
  DIR("inode", "directory", true, ""),

  TEXT_PLAIN(TEXT_TYPE, "plain", false, MediaTypeExtension.TEXT_EXTENSION),

  // According to http://tools.ietf.org/html/rfc4180
  TEXT_CSV(TEXT_TYPE, "csv", false, "csv"),

  TEXT_HTML(TEXT_TYPE, "html", false, "html"),

  TEXT_MD(TEXT_TYPE, "md", false, "md"),
  /**
   * A type used to define relation (in memory data)
   * Tpc
   */
  SQL_RELATION("sql", "relation", false, ""),
  /**
   * A sql file
   */
  TEXT_SQL(TEXT_TYPE, "sql", false, "sql"),

  TEXT_CSS(TEXT_TYPE, "css", false, "css"),

  TEXT_JSON(TEXT_TYPE, "json", false, MediaTypeExtension.JSON_EXTENSION),
  TEXT_JSONL(TEXT_TYPE, "jsonl", false, "jsonl"),
  TEXT_YAML(TEXT_TYPE, "yaml", false, "yml"),
  TEXT_JAVASCRIPT(TEXT_TYPE, "javascript", false, "js"),
  TEXT_XML(TEXT_TYPE, "xml", false, "xml"),
  TEXT_EML(TEXT_TYPE, "eml", false, "eml"),
  EXCEL_FILE(TEXT_TYPE, "xlsx", false, "xlsx");

  private final String type;
  private final String subtype;
  private final boolean isContainer;
  private final String extension;

  MediaTypes(String type, String subType, boolean isContainer, String extension) {

    this.type = type;
    this.subtype = subType;
    this.isContainer = isContainer;
    this.extension = extension;

  }


  /**
   * @param absolutePath an absolute path
   * @return the media Type
   * @throws NotAbsoluteException if the path is not absolute (important to see if this is a directory media type)
   */
  public static MediaType createFromPath(Path absolutePath) throws NotAbsoluteException {

    if (!absolutePath.isAbsolute()) {
      throw new NotAbsoluteException("The path (" + absolutePath + ") is not absolute, we can't determine it media type");
    }

    /**
     * If this is a directory
     */
    if (Files.isDirectory(absolutePath)) {
      return MediaTypes.DIR;
    }

    /**
     * Path/Extension based
     */
    String fullFileName = absolutePath.getFileName().toString();
    int i = fullFileName.lastIndexOf('.');
    String extension;
    if (i != -1) {
      extension = fullFileName.substring(i + 1);
      try {
        return createFromExtension(extension);
      } catch (NullValueException e) {
        // could not happen
        throw new InternalException("This exception should not happen", e);
      }
    }

    /**
     * Name based
     */
    String mediaTypeString = URLConnection.guessContentTypeFromName(absolutePath.getFileName().toString());
    try {
      return createFromMediaTypeString(mediaTypeString);
    } catch (NullValueException e) {
      // null
    }


    /**
     * Special http from content type
     * Http file exists by default
     */
    if (absolutePath.toUri().getScheme().startsWith("http")) {
      try {
        mediaTypeString = absolutePath.toUri().toURL().openConnection().getContentType();
        try {
          return createFromMediaTypeString(mediaTypeString);
        } catch (NullValueException e) {
          // null
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }


    if (!Files.notExists(absolutePath)) {
      return MediaTypes.BINARY_FILE;
    }

    /**
     * Content based
     */
    try {
      /**
       * This is useless if the file has a missing or wrong extension.
       *  It seemed that on windows Files.probeContentType(Path) always returned null.
       *  A major limitation with this is that the file must exist on the file system.
       */
      mediaTypeString = Files.probeContentType(absolutePath);
      try {
        return createFromMediaTypeString(mediaTypeString);
      } catch (NullValueException e) {
        // null
      }

    } catch (IOException e) {
      // Log is depend on the type module unfortunately
      // LoggerType.LOGGER.fine("Error while guessing the mime type of (" + path + ") via probeContent", e.getMessage());
    }

    /**
     * Open and guess content
     */

    /**
     * BufferedInputStream was chosen because it supports marks
     * Otherwise it does not work
     */
    try (InputStream is = new BufferedInputStream(Files.newInputStream(absolutePath))) {
      mediaTypeString = URLConnection.guessContentTypeFromStream(is);
      if (mediaTypeString != null) {
        return createFromMediaTypeString(mediaTypeString);
      }
    } catch (Exception e) {
      /**
       *
       * We may get an error it this is a http url and there is no basic authentication property
       * yet set
       */
      LoggerType.LOGGER.fine("Error while guessing the mime type of (" + absolutePath + ") via content reading", e.getMessage());

    }


    // Unknown
    return MediaTypes.BINARY_FILE;

  }

  /**
   * In a email content mime may be
   * text/plain; charset=utf-8
   */
  public static String getMediaTypeFromMimeType(String value) {

    int firstComma = value.indexOf(";");
    if (firstComma != -1) {
      return value.substring(0, firstComma);
    }
    return value;
  }

  /**
   * @param value a mime from an email
   * @return the media type without any character set
   */
  public static MediaType createFromMimeValue(String value) throws NullValueException {

    String mediaType = getMediaTypeFromMimeType(value);
    return createFromMediaTypeString(mediaType);

  }

  public static MediaType createFromExtension(String fileExtension) throws NullValueException {

    if (fileExtension == null) {
      throw new NullValueException();
    }
    return createFromMediaTypeString("application/" + fileExtension);

  }

  /**
   * @param mediaTypeString the media type string
   * @return a media type
   */
  public static MediaType createFromMediaTypeString(String mediaTypeString) throws NullValueException {

    if (mediaTypeString == null) {
      throw new NullValueException();
    }

    /**
     * Delete characterset if any
     */
    mediaTypeString = getMediaTypeFromMimeType(mediaTypeString);

    /**
     * Processing
     */
    int endIndex = mediaTypeString.indexOf("/");
    mediaTypeString = mediaTypeString.toLowerCase(Locale.ROOT);

    String type;
    String subType;
    if (endIndex != -1) {
      type = mediaTypeString.substring(0, endIndex);
      subType = mediaTypeString.substring(endIndex + 1);
    } else {
      type = null;
      subType = mediaTypeString;
    }

    /**
     * Special case when the user enter text or txt
     */
    if (type == null && (subType.equalsIgnoreCase(TEXT_TYPE) || subType.equalsIgnoreCase("txt"))) {
      return TEXT_PLAIN;
    }

    MediaType mediaTypeObj = new MediaType() {

      @Override
      public String getSubType() {
        return subType;
      }

      @Override
      public String getType() {
        return type;
      }

      @Override
      public boolean isContainer() {
        return false;
      }

      @Override
      public String getExtension() {
        return subType;
      }

      @Override
      public String toString() {
        return this.getType() + "/" + this.getSubType();
      }

    };

    MediaType sameSubtype = null;
    for (MediaType mediaType : values()) {
      if (mediaTypeString.equals(mediaType.toString())) {
        return mediaType;
      }
      if (
        mediaTypeObj.getSubType().equals(mediaType.getSubType()) ||
          mediaTypeObj.getExtension().equals(mediaType.getExtension())
      ) {
        sameSubtype = mediaType;
      }
    }

    if (sameSubtype != null) {
      return sameSubtype;
    }

    return mediaTypeObj;


  }

  /**
   * A function to be used in static construction variable
   *
   * @param s the media type
   * @return a Media Type
   */
  public static MediaType createFromMediaTypeNonNullString(String s) {
    try {
      return createFromMediaTypeString(s);
    } catch (NullValueException e) {
      throw new InternalException("This function should not be filled with a null value");
    }
  }


  @Override
  public String toString() {

    return getType() + "/" + getSubType();
  }

  @Override
  public String getSubType() {
    return this.subtype;
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public boolean isContainer() {
    return this.isContainer;
  }

  @Override
  public String getExtension() {
    return this.extension;
  }


}
