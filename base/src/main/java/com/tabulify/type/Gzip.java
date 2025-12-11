package com.tabulify.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Gzip {

  public static byte[] compress(String plain) {

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){
      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
        gzipOutputStream.write(plain.getBytes(StandardCharsets.UTF_8));
      }
      // gzipOutputStream should be closed to return the full length of bytes
      // otherwise you get Unexpected end of ZLIB input stream
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      // should not happen
      throw new RuntimeException(e);
    }

  }

  /**
   * If you get the below error, the data is not fully available
   * due to network error, ...
   * ```
   * Unexpected end of ZLIB input stream
   * java.io.EOFException: Unexpected end of ZLIB input stream
   * ```
   *
   * @param cipher - the gzip bytes
   */
  public static String decompress(byte[] cipher) throws IOException {


    if ((cipher == null) || (cipher.length == 0)) {
      return "";
    }
    if (!isCompressed(cipher)) {
      throw new IOException("Not Gzip Compressed");
    }

    try (
      ByteArrayInputStream in = new ByteArrayInputStream(cipher);
      final GZIPInputStream gis = new GZIPInputStream(in)) {

      byte[] decompressBytes = gis.readAllBytes();
      return new String(decompressBytes, StandardCharsets.UTF_8);
    }

  }

  public static boolean isCompressed(final byte[] compressed) {
    return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
  }

}
