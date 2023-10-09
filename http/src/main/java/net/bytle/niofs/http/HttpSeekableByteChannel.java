package net.bytle.niofs.http;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * https://docs.oracle.com/javase/8/docs/api/index.html?java/net/HttpURLConnection.html
 * Implemented with a range request
 */
class HttpSeekableByteChannel implements SeekableByteChannel {

  protected static final Logger logger = LoggerFactory.getLogger(HttpSeekableByteChannel.class);


  // The connection Url
  private final HttpRequestPath httpPath;
  // The current position in the connection
  private long currentPosition = 0;
  // The connection (fetch)
  private HttpURLConnection fetch;
  // The channel wrapper around the input stream to read in byte and got easy channel status
  private ReadableByteChannel currentChannel;

  private long byteSize;

  HttpSeekableByteChannel(final HttpRequestPath httpPath) {
    assert httpPath != null : "httpPath cannot be null";
    this.httpPath = httpPath;
    build(currentPosition);

  }

  private void build(long start) {

    fetch = HttpStatic.getHttpFetchObject(httpPath);
    fetch.addRequestProperty("Range", "bytes=" + start + "-");
    fetch.setInstanceFollowRedirects(true);


    /**
     * The size is mandatory for
     * {@link java.nio.file.Files.readAllBytes()}
     * because it creates an array in advance to get the data
     *
     * If the server does not publish it, we read the input stream
     * to determine it
     */
    HttpResponse httpResponse = HttpResponse.createFrom(fetch);
    this.byteSize = httpResponse.getSize();
    currentChannel = Channels.newChannel(new BufferedInputStream(httpResponse.getInputStream()));

  }

  @Override
  public synchronized int read(final ByteBuffer dst) throws IOException {
    // We could also send a range HTTP request
    final int read = currentChannel.read(dst);
    this.currentPosition += read;
    return read;
  }

  @Override
  public int write(ByteBuffer src) {
    throw new NonWritableChannelException();
  }

  @Override
  public synchronized long position() throws IOException {
    if (!isOpen()) {
      throw new ClosedChannelException();
    }
    return currentPosition;
  }

  @Override
  public synchronized HttpSeekableByteChannel position(long newPosition) throws IOException {
    if (newPosition < 0) {
      throw new IllegalArgumentException("Cannot seek a negative position");
    }
    if (!isOpen()) {
      throw new ClosedChannelException();
    }

    if (newPosition > this.currentPosition) {

      final long bytesToSkip = newPosition - this.currentPosition;
      final long skipped = fetch.getInputStream().skip(bytesToSkip);
      logger.debug("Skipped {} bytes out of {} for setting position to {} (previously on {})",
        bytesToSkip, skipped, newPosition, currentPosition);


    } else if (this.currentPosition > newPosition) {

      close();
      build(newPosition);

    }

    this.currentPosition = newPosition;


    return this;
  }

  @Override
  public synchronized long size() throws IOException {


    return this.byteSize;

  }

  @Override
  public SeekableByteChannel truncate(long size) throws IOException {
    throw new NonWritableChannelException();
  }

  @Override
  public synchronized boolean isOpen() {
    return currentChannel.isOpen();
  }

  @Override
  public synchronized void close() throws IOException {
    currentChannel.close();
  }


}
