package net.bytle.db.uri;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Uris {

  public static final String EQUAL_CHARACTER = "=";
  public static final String AMPERSAND_CHARACTER = "&";


  static void print(URI uri) {

    System.out.println("The following URI ("+uri+") is:");
    System.out.println("Opaque            : " + uri.isOpaque());
    System.out.println("Hierarchical      : " + !uri.isOpaque());
    System.out.println("Absolute          : " + uri.isAbsolute());
    System.out.println("Scheme            : " + uri.getScheme());
    System.out.println("SchemeSpecificPart: " + uri.getSchemeSpecificPart());
    System.out.println("Authority         : " + uri.getAuthority());
    System.out.println("Host              : " + uri.getHost());
    System.out.println("UserInfo          : " + uri.getUserInfo());
    System.out.println("Path              : " + uri.getPath());
    System.out.println("Query             : " + uri.getQuery());
    System.out.println("Fragment          : " + uri.getFragment());

  }

  /**
   * @param query
   * @return a set of paramters from a query string
   */
  public static Map<String, String> getQueryAsMap(String query) {
    if (query == null) {
      return new HashMap<>();
    }
    String[] ampersandSplit = query.split(AMPERSAND_CHARACTER);
    Map<String, String> parameters = new HashMap<>();
    for (String ampersand : ampersandSplit) {
      int characterIndex = ampersand.indexOf(EQUAL_CHARACTER);
      if (characterIndex == -1) {
        throw new RuntimeException("The query part (" + ampersand + ") should have at minimum the character (" + EQUAL_CHARACTER + ")");
      }
      String key = ampersand.substring(0, characterIndex);
      String value = ampersand.substring(characterIndex + 1);
      parameters.put(key, value);
    }
    return parameters;
  }

  public static URL getUrl(URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getScheme(String s) {
    if (s == null) {
      throw new RuntimeException("The connection string is null");
    } else {
      int endIndex = s.indexOf(":");
      if (endIndex == -1) {
        return s;
      } else {
        return s.substring(0, endIndex);
      }
    }
  }

  /**
   * Just a wrapper to not have to deal with the exception
   * @param s
   * @return
   */
  public static URI of(String s) {
    try {
      return new URI(s);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
