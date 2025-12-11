package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;

public class UrlWrapperTest {

  @Test
  public void base() throws MalformedURLException {
    // The url for the test
    String originalPath = "/youpla/myfile.csv";
    String schemeHost = "http://example.com:80";
    String queryFragement = "?para=value#yolo";
    String urlString = schemeHost + originalPath + queryFragement;

    /**
     * It could have been create like that
     */
    URL url = new URL("http", "example.com",80, "/youpla/myfile.csv?para=value#yolo");
    Assert.assertEquals(urlString,url.toString());
    Assert.assertEquals(originalPath,url.getPath());

    /**
     * URL wrapper test
     */
    String newPath = "/youplaboum";
    String urlWrapper = UrlWrapper.create(urlString)
      .setPath(newPath)
      .toUrl()
      .toString();
    String urlStringNewPath = schemeHost + newPath + queryFragement;
    Assert.assertEquals(urlStringNewPath,urlWrapper);

  }

  @Test
  public void fileUriTest() {
    String uriString = Paths.get(System.getProperty("user.home")).toUri().toString();
    URI uriViaUrl = UrlWrapper.create(uriString).toURI();
    Assert.assertEquals(uriString,uriViaUrl.toString());
  }

  /**
   * Test with an http url that does not have any non-supported URL character
   */
  @Test
  public void httpUriTest() {
    String urlString = URI.create("https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=xml&prop=description").toString();
    URI uriViaUrl = UrlWrapper.create(urlString).toURI();
    Assert.assertEquals(urlString,uriViaUrl.toString());
  }

  /**
   * Test with an http url that has any non-supported URL character
   */
  @Test
  public void badHttpCharacterTest() throws MalformedURLException {
    String badCharacter = "|";
    String urlEncodedCharacter = "%7C";
    String httpString = "https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=xml&prop=description" + badCharacter;

    /**
     * Creating an URL is working. It takes the bad character
     */
    URL httpUrl = new URL(httpString);

    /**
     * Creating an URI is not working
     */
    Boolean exceptionFired = false;
    try {
      URI uriViaUrl = URI.create(httpString);
    } catch (IllegalArgumentException e){
      exceptionFired = true;
    }
    Assert.assertTrue("The exception was fired",exceptionFired);

    /**
     * In the URL wrapper the URL encoding as occurred
     */
    URL urlWrapped = UrlWrapper.create(httpString).toUrl();
    String encodedURL = httpUrl.toString().replace(badCharacter, urlEncodedCharacter);
    Assert.assertEquals("The url encoded are the same",urlWrapped.toString(), encodedURL);

    /**
     * We can also have an URI because the wrapper encode the value properties
     */
    URI  uriWrapped = UrlWrapper.create(httpString).toURI();
    Assert.assertEquals("The uri encoded are the same",uriWrapped.toString(), encodedURL);


  }

  @Test
  public void alreadyEncoded() {
    String encodedUrl = "https://en.wikipedia.org/w/api.php?Faction%3Dquery%26titles%3DSQL%26format%3Dxml%26prop%3Ddescription%7Ccategories";
    URL urlWrapped = UrlWrapper.create(encodedUrl).toUrl();
    Assert.assertEquals(encodedUrl, urlWrapped.toString());
  }
}
