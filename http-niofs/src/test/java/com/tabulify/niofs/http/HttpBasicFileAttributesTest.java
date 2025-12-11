package com.tabulify.niofs.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;


class HttpBasicFileAttributesTest extends HttpBaseTest {

  @Test
  public void attributeTest() throws IOException, URISyntaxException {

    URL website = new URL("https://example.com/foo/bar");
    Path sourcePath = Paths.get(website.toURI());

    final String myAttrValue = "Mémé dans les orties";
    String attribute = "user:tags";
    Files.setAttribute(sourcePath, attribute, myAttrValue);
    Object value = Files.getAttribute(sourcePath, attribute);
    Assertions.assertNotNull(value);
    Assertions.assertEquals(myAttrValue, value.toString());

  }

  @Test
  public void testSize() throws IOException, URISyntaxException {
    long expectedBytes = 226L;
    URL website = new URL(httpBinUrl + "/range/" + expectedBytes);
    Path sourcePath = Paths.get(website.toURI());
    long size = Files.size(sourcePath);
    Assertions.assertEquals(expectedBytes, size, "Size is good");
  }

  @Test
  public void testLastModifiedSize() throws IOException, URISyntaxException {

    /**
     * Last-Modified: <day-name>, <day> <month> <year> <hour>:<minute>:<second> GMT
     * Wed, 21 Oct 2015 07:28:00 GMT
     */
    URL website = new URL(httpBinUrl + "/response-headers?Last-Modified=Wed%2C%2021%20Oct%202015%2007%3A28%3A00%20GMT");
    Path sourcePath = Paths.get(website.toURI());
    FileTime actualFileTime = Files.getLastModifiedTime(sourcePath);
    FileTime fileTime = FileTime.from(Instant.parse("2015-10-21T07:28:00Z"));
    Assertions.assertEquals(fileTime, actualFileTime, "Size is good");

  }

}
