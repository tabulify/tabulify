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
   *
   * The Content-Length header is not required.
   * Generated pages don't send it, they instead use things like Transfer-Encoding: chunked
   * Where the end of the response is signaled by a zero size chunk; or by forgoing persistent connections (via Connection: close) and simply closing the connection when the response is fully sent.
   *
   * If the request is for a static file, there's a good chance that Content-Length will be present
   * and you can get the size without downloading the file.
   *
   * But in the general case, the only fully viable way is by actually downloading the full response.
   *
   * @param url
   * @return
   */
  static long getSize(URL url) {
    final HttpURLConnection connection;
    try {
      connection = getConnection(url);
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
        throw new RuntimeException("The content-length header was not present in the response. We cannot therefore define the size for the URL ("+url+")");
      }

    } finally {
      connection.disconnect();
    }
    return size;
  }

  /**
   * A wrapper around a connection because the agent is
   * everywhere mandatoru.
   *
   * @param url
   * @return
   */
  static HttpURLConnection getConnection(URL url) {
    try {
      URLConnection urlConnection = url.openConnection();
      if (!(urlConnection instanceof HttpURLConnection))
      {
        throw new RuntimeException("The URL is not using HTTP/HTTPS: " + url);

      }
      HttpURLConnection connection = (HttpURLConnection) urlConnection;
      if (urlConnection instanceof HttpsURLConnection){
        // Trust all certificates
        // TODO: implement it as option
        ((HttpsURLConnection) urlConnection).setSSLSocketFactory(Ssls.getTrustAllCertificateSocketFactory());
      }
      connection.addRequestProperty("User-Agent", HttpFileSystem.USER_AGENT);
      return connection;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
