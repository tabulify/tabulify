package net.bytle.type;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Static function for URL
 * <p>
 * @deprecated Duplicate with {@link UriEnhanced}
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class UrlWrapper {

  private String protocol;
  private String host;
  private int port;
  private String path;
  private String query;
  private String fragment;

  public UrlWrapper(String urlString) {
    if (urlString != null) {
      URL url;
      try {
        url = new URL(urlString);
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }

      this.protocol = url.getProtocol();
      this.host = url.getHost();
      this.port = url.getPort();
      this.path = url.getPath();
      this.query = url.getQuery();
      this.fragment = url.getRef();
    }
  }


  /**
   * @param urlString - a url in a string format
   * @return the object for chaining
   * @throws IllegalArgumentException if the URL is not good
   */
  public static UrlWrapper create(String urlString) {
    return new UrlWrapper(urlString);
  }

  public UrlWrapper setPath(String path) {
    this.path = path;
    return this;
  }

  public String getPath() {
    return this.path;
  }

  public URL toUrl() {
    try {
      String file = "";
      if (path != null) {
        file = path;
      }
      if (query != null) {

        if (query.contains("&")) {
          String queryValueEncoded = (Arrays.stream(query.split("&"))
            .map(prop -> {
              String[] props = prop.split("=");
              return props[0] + "=" + urlEncode(props[1]);
            })
            .collect(Collectors.joining("&")));
          file += "?" + queryValueEncoded;
        } else {
          // Query may be already encoded
          file += "?" + query;
        }
      }
      if (fragment != null) {
        file += "#" + urlEncode(fragment);
      }


      /**
       * This is a hierarchical uri (ie there is no authority)
       * The path should start with two //
       */
      if (this.host.equals("")) {
        file = "//" + file;
      }

      return new URL(
        this.protocol,
        this.host,
        this.port,
        file
      );
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private String urlEncode(String path) {
    return URLEncoder.encode(path, StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    return toUrl().toString();
  }

  public URI toURI() {

    try {
      return toUrl().toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

  }


}
