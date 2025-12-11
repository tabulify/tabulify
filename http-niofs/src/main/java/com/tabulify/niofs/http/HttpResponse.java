package com.tabulify.niofs.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * A wrapper around a connection
 * to keep the data state
 */
public class HttpResponse {

  private final HttpURLConnection fetch;
  private InputStream inputStream;

  public HttpResponse(HttpURLConnection fetch) {
    this.fetch = fetch;
    // Connection send the request
    // as explained here
    // The header fields and the contents of the remote object can be accessed.
    try {
      fetch.connect();
    } catch (IOException e) {
      throw new RuntimeException("Error with the HTTP request. Error:" + e.getMessage(), e);
    }
  }

  public static HttpResponse createFrom(HttpURLConnection fetch) {
    return new HttpResponse(fetch);
  }

  public long getSize() {

    long byteSize = this.fetch.getContentLengthLong();
    if (byteSize != -1) {
      return byteSize;
    }

    try {
      byte[] buff = new byte[8000];
      ByteArrayOutputStream bao = new ByteArrayOutputStream();
      int bytesRead;
      InputStream inputStream = fetch.getInputStream();
      while ((bytesRead = inputStream.read(buff)) != -1) {
        bao.write(buff, 0, bytesRead);
      }
      this.inputStream = new ByteArrayInputStream(bao.toByteArray());
      return bao.size();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public InputStream getInputStream(){
    if(this.inputStream!=null){
      return this.inputStream;
    }
    try {
      return fetch.getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
