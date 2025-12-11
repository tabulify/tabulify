package com.tabulify.niofs.http;

import com.tabulify.niofs.http.HttpHeader;
import com.tabulify.niofs.http.HttpPath;
import com.tabulify.exception.IllegalStructure;
import com.tabulify.type.UriEnhanced;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

class HttpFileTypeDetectorTest {

  @Test
  void wikipediaXmlFileTypeDetectionTest() throws IOException, IllegalStructure, URISyntaxException {

    URI uri = UriEnhanced.createFromString("https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=xml&prop=description|categories").toUri();
    String expected = "text/xml; charset=utf-8";

    /**
     * Via Path
     */
    Path path = Paths.get(uri);
    Assertions.assertEquals(HttpPath.class, path.getClass());
    String contentType = Files.probeContentType(path);
    Assertions.assertEquals(expected, contentType);

    Path wikipediaTarget = Files.createTempFile("wikipedia", ".xml");
    Files.copy(path, wikipediaTarget, StandardCopyOption.REPLACE_EXISTING);

    /**
     * Via Direct call
     */
    HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
    conn.setRequestProperty(HttpHeader.USER_AGENT.toKeyNormalizer().toHttpHeaderCase(), HttpHeader.USER_AGENT.toString());
    conn.setRequestMethod("GET");
    // Check response code
    int responseCode = conn.getResponseCode();
    Assertions.assertEquals(200, responseCode);
    contentType = conn.getContentType();

    Assertions.assertEquals(expected, contentType);
    conn.disconnect();


  }

}
