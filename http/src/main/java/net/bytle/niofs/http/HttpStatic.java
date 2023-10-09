package net.bytle.niofs.http;

import net.bytle.niofs.ssl.Ssls;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * HTTP Static methods
 */
class HttpStatic {

  /**
   * Doc: https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.4
   * <p>
   * The Content-Length header is not required.
   * Generated pages don't send it, they instead use things like Transfer-Encoding: chunked
   * Where the end of the response is signaled by a zero size chunk; or by forgoing persistent connections (via Connection: close) and simply closing the connection when the response is fully sent.
   * <p>
   * If the request is for a static file, there's a good chance that Content-Length will be present
   * and you can get the size without downloading the file.
   * <p>
   * But in the general case, the only fully viable way is by actually downloading the full response.
   *
   * See also {@link HttpResponse#getSize()}
   * @param httpRequestPath - the request path
   * @return the size
   */
  static Long getSizeWithHeadRequest(HttpRequestPath httpRequestPath) {
    final HttpURLConnection connection;
    try {
      connection = getHttpFetchObject(httpRequestPath);
      connection.setRequestMethod("HEAD");
      connection.setRequestProperty("Accept-Encoding", "identity"); // no compression, nor modification
      connection.connect();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    long size;
    try {

      // The Content-Length entity-header field indicates the size of the entity-body,
      // in decimal number of OCTETs, sent to the recipient or, in the case of the HEAD
      // method, the size of the entity-body that would have been sent had the request
      // been a GET.
      size = connection.getContentLengthLong();

      if (size == -1) {
        HttpLog.LOGGER.fine("The content-length header was not present in the response. We cannot therefore define the size for the path (" + httpRequestPath + ")");
        return null;
      }

    } finally {
      connection.disconnect();
    }
    return size;
  }

  /**
   * A wrapper around a connection because the agent is
   * everywhere mandatory.
   *
   * @param httpPath - the path request to fetch
   * @return the connection (fetch object, response comes after connect)
   *
   * TODO: Returns a request
   */
  static HttpURLConnection getHttpFetchObject(HttpRequestPath httpPath) {
    try {

      URL url = httpPath.toUri().toURL();
      URLConnection urlConnection = url.openConnection();
      if (!(urlConnection instanceof HttpURLConnection)) {
        throw new RuntimeException("The URL is not using HTTP/HTTPS: " + url);
      }
      HttpURLConnection connection = (HttpURLConnection) urlConnection;
      if (urlConnection instanceof HttpsURLConnection) {
        // Trust all certificates
        // TODO: implement it as option
        ((HttpsURLConnection) urlConnection).setSSLSocketFactory(Ssls.getTrustAllCertificateSocketFactory());
      }
      connection.addRequestProperty("User-Agent", HttpFileSystem.USER_AGENT);
      if (httpPath.getFileSystem().hasPassword()) {
        connection.addRequestProperty("Authorization", "Basic " + httpPath.getFileSystem().getBasicAuthenticationString());
      }
      if (url.getHost().endsWith("api.mailchimp.com") && !httpPath.getFileSystem().hasPassword()) {
        throw new RuntimeException("A API key should be given as password property for the mailchimp API");
      }
      return connection;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
