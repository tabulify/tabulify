package com.tabulify.type;


import com.tabulify.exception.IllegalStructure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class UriEnhancedTest {

  @Test
  public void UriAsAttributeValue() throws IllegalStructure {
    String uriValue = "https://example.com";
    String uriKey = "uri";
    UriEnhanced uriEnhanced = UriEnhanced
      .create()
      .setScheme("https")
      .setHost("example.com")
      .addQueryProperty(uriKey, uriValue);

    Assertions.assertEquals("https://example.com?uri=https://example.com", uriEnhanced.toUri().toString());

    String urlString = uriEnhanced.toUrl().toString();
    Assertions.assertEquals("https://example.com?uri=https://example.com", urlString);

    String actualUriValue = UriEnhanced.createFromString(urlString)
      .getQueryProperty(uriKey);

    Assertions.assertEquals(uriValue, actualUriValue);


  }

  @Test
  void onlyScheme() throws IllegalStructure {
    String uriString = "mem:/";
    URI uri = URI.create(uriString);
    Assertions.assertEquals("mem", uri.getScheme());
    UriEnhanced uriEnhanced = UriEnhanced.createFromString(uriString);
    Assertions.assertEquals("mem", uriEnhanced.getScheme());
  }

  @Test
  public void UriIllegalCharacter() throws IllegalStructure {
    // the symbol | in the query is illegal, need to be encoded
    UriEnhanced uriEnhanced = UriEnhanced
      .createFromString("https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=json&prop=description|categories");
    Assertions.assertEquals("description|categories", uriEnhanced.getQueryProperty("prop"));
  }

  /**
   * No host with a file uri
   */
  @Test
  public void fileURI() throws IllegalStructure {
    // the symbol | in the query is illegal, need to be encoded
    URI uriEnhanced = UriEnhanced
      .createFromString("file:///my/path").toUri();
    Assertions.assertEquals("/my/path", uriEnhanced.getPath());
  }

  @Test
  public void jdbcUri() throws IllegalStructure {
    // the symbol | in the query is illegal, need to be encoded
    String uriString = "jdbc:sqlite:////home/admin/.tabli/log.db";
    URI uri = URI.create(uriString);
    Assertions.assertEquals("jdbc", uri.getScheme());
    Assertions.assertEquals("sqlite:////home/admin/.tabli/log.db", uri.getSchemeSpecificPart());
    URI uriEnhanced = UriEnhanced
      .createFromString(uriString).toUri();
    Assertions.assertEquals("jdbc", uriEnhanced.getScheme());
  }


}
