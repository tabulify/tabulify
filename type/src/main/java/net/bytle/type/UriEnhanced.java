package net.bytle.type;

import net.bytle.exception.CastException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;

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
  private DnsName host;
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
    URI uri;
    try {
      uri = URI.create(url);
    } catch (Exception e) {
      throw new IllegalStructure("Illegal URL: " + e.getMessage(), e);
    }
    return createFromUri(uri);

  }

  public static UriEnhanced createFromUri(URI uri) {
    UriEnhanced uriEnhanced = new UriEnhanced();
    try {
      return uriEnhanced
        .setScheme(uri.getScheme())
        .setHost(uri.getHost())
        .setPort(uri.getPort())
        .setPath(uri.getPath())
        .setQueryString(uri.getQuery())
        .setFragment(uri.getFragment());
    } catch (IllegalStructure e) {
      throw new RuntimeException("Should not have a problem with the host as the input is an uri", e);
    }

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
    try {
      this.host = DnsName.create(host);
    } catch (CastException e) {
      throw new IllegalStructure("The host value (" + host + ") is not a valid domain name", e);
    }
    return this;
  }

  /**
   * @param authority - the host:port format
   * @return the object for chaining
   */
  private UriEnhanced setAuthority(String authority) throws IllegalStructure {
    String[] authorityParts = authority.split(":");
    this.setHost(authorityParts[0]);
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
    URI uri = this.toUri();
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      if (this.scheme == null) {
        throw new RuntimeException("The scheme is mandatory for a URL (uri: " + uri + ")");
      }
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException("Bad uri to url with the uri (" + uri + "): " + e.getMessage(), e);
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
        return new URI(this.scheme, this.host.toStringWithoutRoot(), this.path, nonEscapedQueryString, this.fragment);
      } else {
        return new URI(this.scheme, null, this.host.toStringWithoutRoot(), this.port, this.path, nonEscapedQueryString, this.fragment);
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param withEncoding - false if you use {@link URI}
   * @return the string encoded or not
   */
  @SuppressWarnings("SameParameterValue")
  private String getQueryString(boolean withEncoding) {
    if (this.queryProperties.isEmpty()) {
      return null;
    }
    StringBuilder query = new StringBuilder();
    boolean firstIsPassed = false;
    for (Map.Entry<String, String> queryProperty : this.queryProperties.entrySet()) {
      if (firstIsPassed) {
        query.append(AMPERSAND_CHARACTER);
      } else {
        firstIsPassed = true;
      }
      String key = queryProperty.getKey();
      if (withEncoding) {
        key = URLEncoder.encode(key, StandardCharsets.UTF_8);
      }
      query.append(key);
      String value = queryProperty.getValue();
      if (value != null) {
        if (withEncoding) {
          value = URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        query.append(EQUAL_CHARACTER).append(value);
      }
    }
    return query.toString();
  }

  public DnsName getHost() throws NotFoundException {
    if(this.host==null){
      throw new NotFoundException();
    }
    return this.host;
  }

  /**
   * @deprecated use {@link #getHost()}
   * @return the host or null if not found
   */
  @Deprecated
  public String getHostAsString() {
    if (this.host == null) {
      return null;
    }
    return this.host.toStringWithoutRoot();
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
      key = URLDecoder.decode(key, StandardCharsets.UTF_8);
      if (value != null) {
        value = URLDecoder.decode(value, StandardCharsets.UTF_8);
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

  @SuppressWarnings("unused")
  public String getFragment() {
    return this.fragment;
  }

  /**
   * @return the host with the port if known
   * Some API consider the port to be part of the host and other not.
   * We consider not because for use the host is the DNS name
   */
  public String getHostWithPort() {
    if (port == null) {
      return this.host.toStringWithoutRoot();
    }
    return this.host.toStringWithoutRoot() + ":" + port;
  }


  @Override
  public String toString() {
    return toUri().toString();
  }

  public UriEnhanced addQueryProperties(Map<String, String> queriesProperties) {

    for (Map.Entry<String, String> entry : queriesProperties.entrySet()) {
      this.addQueryProperty(entry.getKey(), entry.getValue());
    }
    return this;

  }
}
