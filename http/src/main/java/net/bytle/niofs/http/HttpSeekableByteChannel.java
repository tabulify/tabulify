package net.bytle.niofs.http;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
  private final HttpPath httpPath;
  // The current position in the connection
  private long currentPosition = 0;
  // The connection
  private HttpURLConnection currentConnection;
  // The channel wrapper around the input stream to read in byte and got easy channel status
  private ReadableByteChannel currentChannel;

  HttpSeekableByteChannel(final HttpPath httpPath) {
    assert httpPath != null : "httpPath cannot be null";
    this.httpPath = httpPath;
    build(currentPosition);

  }

  private void build(long start) {
    try {
      currentConnection = HttpStatic.getConnection(httpPath.getUrl());
      currentConnection.addRequestProperty("Range", "bytes=" + start + "-");
      currentChannel = Channels.newChannel(currentConnection.getInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    return new HttpBasicFileAttributes(httpPath).size();
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
