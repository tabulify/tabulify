package com.tabulify.type;

import com.tabulify.exception.InternalException;
import com.tabulify.exception.NotAbsoluteException;
import com.tabulify.exception.NullValueException;
import com.tabulify.text.plain.TextCharacterSetNotDetected;
import com.tabulify.text.plain.TextDetectedCharsetNotSupported;
import com.tabulify.text.plain.TextFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.tabulify.type.MediaType.TEXT_TYPE;

/**
 * A collection of well known MediaType / Mime
 * and of static constructors
 */
@SuppressWarnings("unused")
public class MediaTypes {


    static Set<MediaType> standardizeDataTypes;


    static public MediaType BINARY_FILE = new MediaTypeAbs() {

        @Override
        public String getType() {
            return "application";
        }

        @Override
        public String getSubType() {
            return "octet-stream";
        }

        @Override
        public boolean isContainer() {
            return false;
        }

        @Override
        public String getExtension() {
            return "bin";
        }

    };

    // Ubuntu
    // https://stackoverflow.com/questions/18869772/mime-type-for-a-directory
    static public MediaType DIR = new MediaTypeAbs() {

        @Override
        public String getType() {
            return "inode";
        }

        @Override
        public String getSubType() {
            return "directory";
        }

        @Override
        public boolean isContainer() {
            return true;
        }

        @Override
        public String getExtension() {
            return "";
        }

    };

    static public MediaType TEXT_PLAIN = new MediaTypeText() {


        @Override
        public String getSubType() {
            return "plain";
        }

        @Override
        public String getExtension() {
            return MediaTypeExtension.TEXT_EXTENSION;
        }

    };

    // According to http://tools.ietf.org/html/rfc4180
    static public MediaType TEXT_CSV = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "csv";
        }

    };

    static public MediaType TEXT_HTML = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "html";
        }


    };

    static public MediaType TEXT_MD = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "md";
        }

    };
    static public MediaType TEXT_LOG = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "log";
        }

    };
    /**
     * A type used to define relation (in memory data)
     * Tpc
     */
    static public MediaType SQL_RELATION = new MediaTypeAbs() {

        @Override
        public String getType() {
            return "sql";
        }

        @Override
        public String getSubType() {
            return "relation";
        }


    };
    /**
     * A sql file
     */
    static public MediaType TEXT_SQL = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "sql";
        }

    };

    static public MediaType TEXT_CSS = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "css";
        }


    };

    static public MediaType TEXT_JSON = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "json";
        }

    };
    static public MediaType TEXT_JSONL = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "jsonl";
        }

    };

    static public MediaType TEXT_YAML = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "yaml";
        }

        @Override
        public String getExtension() {
            return "yml";
        }
    };

    static public MediaType TEXT_JAVASCRIPT = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "javascript";
        }

        @Override
        public String getExtension() {
            return "js";
        }

    };

    static public MediaType TEXT_XML = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "xml";
        }

    };

    static public MediaType TEXT_EML = new MediaTypeText() {

        @Override
        public String getSubType() {
            return "eml";
        }

    };


    static {
        standardizeDataTypes = new HashSet<>();
        standardizeDataTypes.add(TEXT_EML);
        standardizeDataTypes.add(TEXT_CSS);
        standardizeDataTypes.add(TEXT_CSV);
        standardizeDataTypes.add(TEXT_HTML);
        standardizeDataTypes.add(DIR);
        standardizeDataTypes.add(TEXT_JAVASCRIPT);
        standardizeDataTypes.add(TEXT_JSONL);
        standardizeDataTypes.add(TEXT_JSON);
        standardizeDataTypes.add(TEXT_LOG);
        standardizeDataTypes.add(TEXT_PLAIN);
        standardizeDataTypes.add(TEXT_MD);
        standardizeDataTypes.add(TEXT_SQL);
        standardizeDataTypes.add(TEXT_XML);
        standardizeDataTypes.add(TEXT_YAML);
    }

    /**
     * Same as {@link #detectMediaType(Path)} but you are sure that the path is absolute
     *
     * @throws InternalException if this is not the case
     */
    public static MediaType detectMediaTypeSafe(Path path) {

        try {
            return MediaTypes.detectMediaType(path);
        } catch (NotAbsoluteException e) {
            throw new InternalException("The path (" + path + ") is not absolute", e);
        }

    }

    /**
     * @param absolutePath an absolute path
     * @return the media/content type string that is somewhat normalized
     * @throws NotAbsoluteException if the path is not absolute (important to see if this is a directory media type)
     *                              We don't return an object because MediaType object are created by the type manager
     *                              because not all types are known in advance, and they are normally enum
     *                              If you want to detect your own media type, you should implement a {@link FileTypeDetector}
     */
    public static MediaType detectMediaType(Path absolutePath) throws NotAbsoluteException {

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
         * File System based
         * They need to implement java.nio.file.spi.FileTypeDetector
         */
        String mediaTypeString;
        try {
            /**
             * May be implemented
             */
            mediaTypeString = Files.probeContentType(absolutePath);
            try {
                return createFromString(mediaTypeString);
            } catch (NullValueException e) {
                // mediaTypeString may be null if not detected
            }

        } catch (IOException e) {
            // Log is depend on the type module unfortunately
            // LoggerType.LOGGER.fine("Error while guessing the mime type of (" + path + ") via probeContent", e.getMessage());
        }


        /**
         * Name based
         */
        Path fileName = absolutePath.getFileName();
        if (fileName == null) {
            // file system may not have any name in the path for file
            // (ie http has no directory only file, but they may have no name. Example: https://example.com)
            throw new RuntimeException("The file (" + absolutePath + ") does not have any name");
        }

        /**
         * Extension based
         */
        String fullFileName = fileName.toString();
        int i = fullFileName.lastIndexOf('.');
        String extension;
        if (i != -1) {
            extension = fullFileName.substring(i + 1);
            try {
                return getFromExtension(extension);
            } catch (NullValueException e) {
                // could not happen
                throw new InternalException("This exception should not happen", e);
            }
        }


        /**
         * Name based
         */
        mediaTypeString = URLConnection.guessContentTypeFromName(fileName.toString());
        try {
            return createFromString(mediaTypeString);
        } catch (NullValueException e) {
            // null
        }


        if (!Files.exists(absolutePath)) {
            return MediaTypes.BINARY_FILE;
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
                return createFromString(mediaTypeString);
            }
        } catch (Exception e) {
            /**
             *
             * We may get an error it this is a http url and there is no basic authentication property
             * yet set
             */
            LoggerType.LOGGER.fine(() -> "Error while guessing the mime type of (" + absolutePath + ") via content reading. Message: " + e.getMessage());

        }

        /**
         * Try to return a text (Charset to verify that this is a text file)
         */
        try {
            TextFile.builder(absolutePath).build();
            return MediaTypes.TEXT_PLAIN;
        } catch (TextDetectedCharsetNotSupported e) {
            // charset detected but not supported
            return MediaTypes.TEXT_PLAIN;
        } catch (TextCharacterSetNotDetected e) {
            //
        }


        // Unknown
        return MediaTypes.BINARY_FILE;

    }


    /**
     * @param value a mime from an email
     * @return the media type without any character set
     * In an email content mime may be
     * text/plain; charset=utf-8
     */
    public static String getFromMimeValue(String value) {

        int firstComma = value.indexOf(";");
        if (firstComma != -1) {
            return value.substring(0, firstComma);
        }
        return value;

    }

    public static MediaType getFromExtension(String fileExtension) throws NullValueException {

        if (fileExtension == null) {
            throw new NullValueException();
        }
        return parse(fileExtension);

    }

    public static MediaType createFromString(String mediaTypeString) throws NullValueException {
        return parse(mediaTypeString);
    }

    /**
     * Wrapper of {@link #parse(String)} that does not throw any exception
     * Be sure to not pass a null of empty value
     */
    public static MediaType parseSafe(String string) {
        try {
            return parse(string);
        } catch (NullValueException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For whatever reason, Java does not pick {@link #equals(MediaType, Object)}
     * and throws a: java.lang.NoSuchMethodError: 'boolean net.bytle.type.MediaTypes.equals(net.bytle.type.MediaType, net.bytle.type.MediaType)'
     */
    public static boolean equals(MediaType mediaType1, MediaType mediaType2) {
        return equals(mediaType1, (Object) mediaType2);
    }

    /**
     * Because media type may be enum, we need this equality
     * static function
     */
    public static boolean equals(MediaType mediaType, Object o) {

        if (mediaType == o) return true;
        if (!(o instanceof MediaType)) return false;
        MediaType objectMediaType = (MediaType) o;
        return mediaType.getType().equals(objectMediaType.getType()) && mediaType.getSubType().equals(objectMediaType.getSubType());

    }

    /**
     * Parse the string and returns a dynamically created MediaType
     *
     * @param string a string (a content type, a media type, a mime type or an extension)
     * @return a media type
     * @throws NullValueException if the value is null or empty
     */
    public static MediaType parse(String string) throws NullValueException {

        if (string == null || string.isEmpty()) {
            throw new NullValueException();
        }

        /**
         * Delete character set if any
         */
        string = getFromMimeValue(string);

        /**
         * Processing
         */
        int endIndex = string.indexOf("/");
        string = string.toLowerCase(Locale.ROOT);

        String type;
        String subType;
        if (endIndex != -1) {
            type = string.substring(0, endIndex);
            subType = string.substring(endIndex + 1);
        } else {
            type = "";
            subType = string;
        }

        /**
         * Special case when the user enter text or txt
         */
        if (type.isEmpty() && (subType.equalsIgnoreCase(TEXT_TYPE) || subType.equalsIgnoreCase("txt"))) {
            return TEXT_PLAIN;
        }

        MediaType mediaTypeObj = new MediaTypeAbs() {

            @Override
            public String getSubType() {
                return subType;
            }

            @Override
            public String getType() {
                return type;
            }

        };

        MediaType sameSubtype = null;

        for (MediaType mediaType : standardizeDataTypes) {
            if (string.equals(mediaType.toString())) {
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
            return createFromString(s);
        } catch (NullValueException e) {
            throw new InternalException("This function should not be filled with a null value");
        }
    }


}
