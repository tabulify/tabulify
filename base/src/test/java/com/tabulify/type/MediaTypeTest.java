package com.tabulify.type;


import com.tabulify.exception.NullValueException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MediaTypeTest {

    @Test
    public void fromMime() {

        String validEmailValue = "text/plain; charset=utf-8";
        String mediaType = MediaTypes.getFromMimeValue(validEmailValue);
        Assertions.assertEquals("text/plain", mediaType);

    }

    @Test
    public void parsing() throws NullValueException {

        /**
         * Parse a goof media/content type
         */
        MediaType mediaType = MediaTypes.parse("text/plain");
        Assertions.assertEquals("text", mediaType.getType());
        Assertions.assertEquals("plain", mediaType.getSubType());
        Assertions.assertEquals("text/plain", mediaType.toString());

        /*
         * Parse by known extension
         */
        mediaType = MediaTypes.parse("csv");
        Assertions.assertEquals("text", mediaType.getType());
        Assertions.assertEquals("csv", mediaType.getSubType());
        Assertions.assertEquals("text/csv", mediaType.toString());

        /*
         * A single word should have a null type
         */
        mediaType = MediaTypes.parse("yolo");
        Assertions.assertEquals("", mediaType.getType());
        Assertions.assertEquals("yolo", mediaType.getSubType());

    }


}
