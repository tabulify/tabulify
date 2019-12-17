package net.bytle.niofs.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * https://docs.oracle.com/javase/8/docs/api/index.html?java/net/HttpURLConnection.html
 * Implemented with a range request
 */
class HttpSeekableByteChannel implements SeekableByteChannel {

  private static final String USER_AGENT = "Bytle NioFs Http";

  static Logger logger = LoggerFactory.getLogger(HttpSeekableByteChannel.class);


  // The connection Url
  private final URL url;
  // The current position in the connection
  private long currentPosition = 0;
  // The connection
  private HttpURLConnection currentConnection;
  // The channel wrapper around the input stream to read in byte and got easy channel status
  private ReadableByteChannel currentChannel;

  HttpSeekableByteChannel(final HttpPath httpPath) {
    assert httpPath != null : "httpPath cannot be null";
    this.url = httpPath.getUrl();
    build(currentPosition);

  }

  private void build(long start) {
    try {
      currentConnection = (HttpURLConnection) url.openConnection();
      currentConnection.addRequestProperty("User-Agent", USER_AGENT);
      currentConnection.addRequestProperty("Range", "bytes=" + start + "-");
      currentChannel = Channels.newChannel(currentConnection.getInputStream());
    } catch (IOException e) {
     throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized int read(final ByteBuffer dst) throws IOException {
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
      final long skipped = currentConnection.getInputStream().skip(bytesToSkip);
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


    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("HEAD");
    connection.connect();
    long size;
    try {

      // The Content-Length entity-header field indicates the size of the entity-body,
      // in decimal number of OCTETs, sent to the recipient or, in the case of the HEAD
      // method, the size of the entity-body that would have been sent had the request
      // been a GET.
      size = connection.getContentLengthLong();

      if (size == -1) {
        throw new IOException("Unable to retrieve content length for " + url);
      }

    } finally {
      connection.disconnect();
    }

    return size;
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
