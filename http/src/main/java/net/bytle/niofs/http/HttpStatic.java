package net.bytle.niofs.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP Static methods
 */
class HttpStatic {

  static long getSize(URL url) {
    final HttpURLConnection connection;
    try {
      connection = getConnection(url);
      connection.setRequestMethod("HEAD");
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
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("User-Agent", HttpFileSystem.USER_AGENT);
      return connection;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
