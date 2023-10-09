package net.bytle.type;

import net.bytle.exception.CastException;
import net.bytle.exception.IllegalStructure;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A URI wrapper to be able to build and get URI data
 * <p>
 * that java does not provide such as key case and separation key independence
 */
public class UriEnhanced {


  public static final String AMPERSAND_CHARACTER = "&";
  public static final String EQUAL_CHARACTER = "=";
  private String scheme;
  /**
   * The host does not have any port information
   */
  private String host;
  private Integer port = null;
  private MapKeyIndependent<String> queryProperties = new MapKeyIndependent<>();
  private String fragment;
  private String path;

  public static UriEnhanced create() {
    return new UriEnhanced();
  }

  public static UriEnhanced createFromString(String url) throws IllegalStructure {
    if (url == null) {
      throw new IllegalStructure("The url cannot be null");
    }
    UriEnhanced uriEnhanced = UriEnhanced.create();
    URI uri = URI.create(url);
    return uriEnhanced
      .setScheme(uri.getScheme())
      .setHost(uri.getHost())
      .setPort(uri.getPort())
      .setPath(uri.getPath())
      .setQueryString(uri.getQuery())
      .setFragment(uri.getFragment());
  }

  public UriEnhanced setFragment(String fragment) {
    this.fragment = fragment;
    return this;
  }

  public UriEnhanced setQueryString(String queryString) {
    this.queryProperties = UriEnhanced.getQueryAsMap(queryString);
    return this;
  }

  public UriEnhanced setPath(String path) {
    this.path = path;
    return this;
  }

  public UriEnhanced setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  /**
   * @param host - the host (or the authority, most webserver see the host as the authority)
   * @return the object for chaining
   */
  public UriEnhanced setHost(String host) throws IllegalStructure {
    if (host == null) {
      return this;
    }
    if (host.contains(":")) {
      return this.setAuthority(host);
    }
    this.host = host;
    return this;
  }

  /**
   * @param authority - the host:port format
   * @return the object for chaining
   */
  private UriEnhanced setAuthority(String authority) throws IllegalStructure {
    String[] authorityParts = authority.split(":");
    this.host = authorityParts[0];
    if (authorityParts.length >= 2) {
      String authorityPort = authorityParts[1];
      try {
        this.port = Casts.cast(authorityPort, Integer.class);
      } catch (CastException e) {
        throw new IllegalStructure("The authority port (" + authorityPort + ") of (" + authority + ") is not an integer");
      }
    }
    return this;
  }

  public UriEnhanced setPort(int port) {
    this.port = port;
    return this;
  }


  public UriEnhanced addQueryProperty(String key, String value) {
    this.queryProperties.put(key, value);
    return this;
  }

  public UriEnhanced addQueryProperty(Enum<?> key, String value) {
    addQueryProperty(key.toString(), value);
    return this;
  }

  public URL toUrl() {
    try {
      return this.toUri().toURL();
    } catch (MalformedURLException e) {
      if (this.scheme == null) {
        throw new RuntimeException("The scheme is mandatory for a URL");
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the object for fluency
   * @throws IllegalStructure - if the URI is not a valid origin
   */
  @SuppressWarnings("unused")
  public UriEnhanced validateAsOrigin() throws IllegalStructure {
    if (!this.queryProperties.isEmpty()) {
      throw new IllegalStructure("An origin URI does not have any query properties");
    }
    if (fragment != null) {
      throw new IllegalStructure("An origin URI does not have any fragment");
    }
    return this;
  }

  public URI toUri() {
    try {
      /**
       * The URI query string is quoted in the URI constructor
       */
      String nonEscapedQueryString = this.getQueryString(false);
      if (this.port == null) {
        return new URI(this.scheme, this.host, this.path, nonEscapedQueryString, this.fragment);
      } else {
        return new URI(this.scheme, null, this.host, this.port, this.path, nonEscapedQueryString, this.fragment);
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param withEncoding - false if you use {@link URI}
   * @return the string encoded or not
   */
  private String getQueryString(boolean withEncoding) {
    if (this.queryProperties.size() == 0) {
      return null;
    }
    StringBuilder query = new StringBuilder();
    boolean firstIsPassed = false;
    for (Map.Entry<String, String> queryProperty : this.queryProperties.entrySet()) {
      try {
        if (firstIsPassed) {
          query.append(AMPERSAND_CHARACTER);
        } else {
          firstIsPassed = true;
        }
        String key = queryProperty.getKey();
        if (withEncoding) {
          key = URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        }
        query.append(key);
        String value = queryProperty.getValue();
        if (value != null) {
          if (withEncoding) {
            value = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
          }
          query.append(EQUAL_CHARACTER).append(value);
        }
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return query.toString();
  }

  public String getHost() {
    return this.host;
  }

  public Integer getPort() {
    return this.port;
  }

  public String getQueryProperty(String key) {
    return this.queryProperties.get(key);
  }

  /**
   * @param query the query string
   * @return a map with a set of parameters from a query string
   */
  protected static MapKeyIndependent<String> getQueryAsMap(String query) {
    if (query == null) {
      return new MapKeyIndependent<>();
    }
    String[] queryParts = query.split(UriEnhanced.AMPERSAND_CHARACTER);
    MapKeyIndependent<String> parameters = new MapKeyIndependent<>();
    for (String queryPart : queryParts) {
      int characterIndex = queryPart.indexOf(UriEnhanced.EQUAL_CHARACTER);
      String key = queryPart;
      String value = null;
      if (characterIndex != -1) {
        key = queryPart.substring(0, characterIndex);
        value = queryPart.substring(characterIndex + 1);
      }
      try {
        key = URLDecoder.decode(key, StandardCharsets.UTF_8.name());
        if (value != null) {
          value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        }
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

      parameters.put(key, value);
    }
    return parameters;
  }

  public String getQueryProperty(Enum<?> enumValue) {
    return getQueryProperty(enumValue.toString());
  }

  public String getScheme() {
    return this.scheme;
  }

  public String getPath() {
    return this.path;
  }

  /**
   * @return a string or null
   */
  public String getQuery() {
    return this.getQueryString(false);
  }

  public String getFragment() {
    return this.fragment;
  }

  /**
   * @return the host with the port if known
   * Some API consider the port to be part of the host and other not.
   */
  public String getHostWithPort() {
    if (port == null) {
      return getHost();
    }
    return getHost() + ":" + port;
  }

  /**
   * @return the apex domain (ie the two last name of the host)
   * in `member.combostrap.com` the apex is the `combostrap.com`
   */
  public String getApexWithoutPort() {
    String host = getHost();
    if (host == null) {
      return "";
    }
    String[] hostNames = getHost().split("\\.");
    int length = hostNames.length;
    switch (length) {
      case 1:
        return hostNames[0];
      default:
        return hostNames[length - 2] + "." + hostNames[length - 1];
    }
  }

  /**
   * @return the subdomain (ie on `foo.bar.example.com` returns `foo.bar`) or empty string
   */
  public String getSubDomain() {
    if (host == null || host.equals("")) {
      return "";
    }
    int apexLength = getApexWithoutPort().length();
    if (host.length() <= apexLength) {
      return "";
    }
    return host.substring(0, host.length() - apexLength - 1);
  }
}
