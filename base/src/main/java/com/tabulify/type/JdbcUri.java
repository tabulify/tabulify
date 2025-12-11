package com.tabulify.type;

import com.tabulify.exception.CastException;
import com.tabulify.exception.IllegalStructure;
import com.tabulify.exception.NotFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Not all JDBC URL are URI compliant, this class wraps this fact
 * Actually they are, the scheme is taken and the rest goes into {@link URI}
 * Not true, they are
 */
public class JdbcUri {

  private static final Logger LOGGER = Logger.getLogger(JdbcUri.class.getPackage().getName());
  private UriEnhanced sqlUri;


  private URI uri;

  public JdbcUri(URI uri) {


    this.uri = uri;

    // parsing
    // we can't create another URI thanks to weird URI of SQL Server
    // jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;encrypt=true;trustServerCertificate=true
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    try {
      sqlUri = parseURI(schemeSpecificPart);
    } catch (CastException ex) {
      throw new RuntimeException(ex);
    }

  }

  public JdbcUri(String url) {
    try {
      this.uri = new URI(url);
    } catch (URISyntaxException e) {
      // The Sqlite URL is not URI compliant
      // jdbc:sqlite:///C:\\Users\\gerard\\AppData\\Local\\Temp\\bytle-db\\defaultDb.db
      // should be written jdbc:sqlite:///C:/Users/defaultDb.db
      final String msg = "The URL (" + url + ") is not URI compliant.";
      LOGGER.severe(msg);
      System.err.println(msg);
      throw new RuntimeException(e);
    }
  }

  public String getDriver() {


    String driver = null;

    String server = getSqlScheme();
    if (server == null) {
      return null;
    }

    switch (server) {
      case "sap":
        driver = "com.sap.db.jdbc.Driver";
        break;
      case "oraclebi":
        driver = "oracle.bi.jdbc.AnaJdbcDriver";
        break;
      case "timesten":
        driver = "com.timesten.jdbc.TimesTenDriver";
        break;
      default:
        LOGGER.fine("The driver for the server (" + server + ") is not known.");
    }

    return driver;

  }

  public String getSqlScheme() {
    return sqlUri.getScheme();
  }

  public String getPath() {
    return sqlUri.getPath();
  }

  public DnsName getHost() {
    try {
      return sqlUri.getHost();
    } catch (NotFoundException e) {
      return null;
    }
  }

  public static UriEnhanced parseURI(String uri) throws CastException {


    // Variables to store the different parts of the URI
    StringBuilder scheme = new StringBuilder();
    StringBuilder afterScheme = new StringBuilder();
    StringBuilder host = new StringBuilder();
    StringBuilder port = new StringBuilder();
    StringBuilder path = new StringBuilder();
    StringBuilder queryString = new StringBuilder();
    Map<String, String> queryParams = new HashMap<>();

    // State tracking variables
    boolean inScheme = true;
    boolean schemeEnded = false;
    boolean inHost = false;
    boolean inPort = false;
    boolean inPath = false;
    boolean inQuery = false;
    boolean inQueryKey = false;
    boolean inQueryValue = false;

    // Current key and value for query parameters
    StringBuilder currentKey = new StringBuilder();
    StringBuilder currentValue = new StringBuilder();

    // Parse the URI character by character
    for (int i = 0; i < uri.length(); i++) {
      char c = uri.charAt(i);

      // Scheme parsing
      if (inScheme) {
        if (c == ':') {
          inScheme = false;
          schemeEnded = true;
          continue;
        }
        scheme.append(c);
      }

      // After scheme, expect "//" or oracle "thin:@"
      else if (schemeEnded && !inHost && !inPort && !inPath && !inQuery) {
        if (c != '/' && c != '@') {
          afterScheme.append(c);
          continue;
        }
        if (c == '/') {
          i++; // Skip the next slash
        }
        inHost = true;
      }

      // Host parsing
      else if (inHost) {
        if (c == ':') {
          inHost = false;
          inPort = true;
          continue;
        } else if (c == '/') {
          inHost = false;
          inPath = true;
          path.append(c);
          continue;
        }
        host.append(c);
      }

      // Port parsing
      else if (inPort) {
        if (!Character.isDigit(c)) {
          inPort = false;
          inPath = true;
          path.append(c);
          continue;
        }
        port.append(c);
      }

      // Path parsing
      else if (inPath) {
        if (c == '?') {
          inPath = false;
          inQuery = true;
          inQueryKey = true;
          continue;
        }
        path.append(c);
      }

      // Query parsing
      else if (inQuery) {
        queryString.append(c);

        if (inQueryKey) {
          if (c == '=') {
            inQueryKey = false;
            inQueryValue = true;
            continue;
          }
          currentKey.append(c);
        } else if (inQueryValue) {
          if (c == '&') {
            // Store the completed key-value pair
            queryParams.put(currentKey.toString(), currentValue.toString());

            // Reset for the next key-value pair
            currentKey = new StringBuilder();
            currentValue = new StringBuilder();
            inQueryKey = true;
            inQueryValue = false;
            continue;
          }
          currentValue.append(c);
        }
      }
    }

    // Add the last key-value pair if we ended in a query value
    if (inQueryValue && currentKey.length() > 0) {
      queryParams.put(currentKey.toString(), currentValue.toString());
    }


    UriEnhanced uriEnhanced = UriEnhanced
      .create()
      .setScheme(scheme.toString())
      .addQueryProperties(queryParams);
    if (host.length() > 0) {
      String hostString = host.toString();
      try {
        uriEnhanced.setHost(hostString);
      } catch (IllegalStructure e) {
        throw new CastException("The host string value (" + hostString + " ) is not a valid hostname", e);
      }
    }
    if (port.length() > 0) {
      Integer cast;
      String portString = port.toString();
      try {
        cast = Casts.cast(portString, Integer.class);
      } catch (CastException e) {
        throw new CastException("The port string value (" + portString + " ) is not a valid integer", e);
      }
      uriEnhanced.setPort(cast);
    }
    if (path.length() > 0) {
      uriEnhanced.setPath(path.toString());
    }
    return uriEnhanced;


  }

  public String getScheme() {
    return uri.getScheme();
  }

  public Integer getPort() {
    return sqlUri.getPort();
  }

  @Override
  public String toString() {
    return uri.toString();
  }

}
