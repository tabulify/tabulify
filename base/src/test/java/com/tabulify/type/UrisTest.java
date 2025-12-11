package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class UrisTest {


  /**
   * What information do we got within a file URI
   * This is a hierarchical URI
   */
  @Test
  public void FsUriTest() throws URISyntaxException {

    final String query = "a=b&b=c";
    // The path must be absolute
    // Example windows
    final String path = "/D:/youolo";
    // Example Linux
    // final String path = "/youolo";

    final String scheme = "file";
    final String fragment = "part1";
    String uri = scheme + "://" + path + "?" + query + "#" + fragment;
    URI uris = new URI(uri);
    Assert.assertEquals("schema", scheme, uris.getScheme());
    Assert.assertEquals("path", path, uris.getPath());
    Assert.assertNull("host", uris.getHost());
    Assert.assertNull("authority", uris.getAuthority());
    Assert.assertFalse("opaque", uris.isOpaque());
    Assert.assertEquals("sql", query, uris.getQuery());
    Assert.assertEquals("fragment", fragment, uris.getFragment());
    Uris.print(uris);

  }


  /**
   * In a Opaque URI, only the scheme is parsed, the rest is going into the scheme specific part
   */
  @Test
  public void MailOpaqueUriTest() throws URISyntaxException {

    final String mailScheme = "mailto";
    final String specificPart = "John.Doe@example.com";
    String uriString = mailScheme + ":" + specificPart;
    URI uri = new URI(uriString);
    Assert.assertEquals("scheme", mailScheme, uri.getScheme());
    Assert.assertEquals("specificPart", specificPart, uri.getSchemeSpecificPart());
    Assert.assertTrue("isOpaque", uri.isOpaque());

    // The rest is null
    Assert.assertNull("path", uri.getPath());
    Assert.assertNull("host", uri.getHost());
    Assert.assertNull("authority", uri.getAuthority());
    Uris.print(uri);


  }

  /**
   * A Jdbc is not a URI. Some are if we tweak the first part
   */
  @Test
  public void JdbcUriTest() throws URISyntaxException {


    String jdbcUri = "postgresql://host:port/test?ssl=true";
    String fullJdbcUri = "jdbc:" + jdbcUri;
    // ie Delete jdbc
    String cleanURI = fullJdbcUri.substring(5);

    URI uri = new URI(cleanURI);
    Assert.assertEquals("schema", "postgresql", uri.getScheme());
    Assert.assertFalse("opaque", uri.isOpaque());
    Assert.assertTrue("absolute", uri.isAbsolute());
    Assert.assertEquals("specificPart", "//host:port/test?ssl=true", uri.getSchemeSpecificPart());
    Assert.assertNull("host", uri.getHost());
    Assert.assertEquals("authority", "host:port", uri.getAuthority());
    Assert.assertEquals("path", "/test", uri.getPath());

    Uris.print(uri);

  }

  /**
   * Zip
   */
  @Test
  public void ZipUriTest() throws URISyntaxException {


    URI uri = new URI("zip:https://example.com/file.zip");
    Uris.print(uri);
    uri = new URI("zip:file://./build/example.zip");
    Uris.print(uri);

  }
}
