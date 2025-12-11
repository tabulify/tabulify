package com.tabulify.niofs.http;

import com.tabulify.exception.NotAbsoluteException;
import com.tabulify.fs.Fs;
import com.tabulify.type.MediaTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.HashMap;


public class HttpFileSystemTest extends HttpBaseTest {


  @Test
  public void testNewFileSystem() throws IOException, URISyntaxException {

    URL website = new URL(httpBinUrl + "/html");

    try (FileSystem fileSystem = FileSystems.newFileSystem(website.toURI(), new HashMap<>())) {
      Assertions.assertEquals(HttpFileSystem.class, fileSystem.getClass());
      HttpFileSystem httpFileSystem = (HttpFileSystem) fileSystem;
      String workingPath = httpFileSystem.getWorkingStringPath();
      String expectedWorkingPath = "/html";
      Assertions.assertEquals(expectedWorkingPath, workingPath);
    }


  }

  /**
   * We can't download from <a href="https://downloads.mysql.com/docs/world-db.zip">...</a>
   * if the {@link HttpHeader#USER_AGENT} is not a known user agent such as curl
   * We get a 403 otherwise
   */
  @Test
  public void testUserAgentOnMySql() throws IOException, URISyntaxException {

    Path sourcePath = HttpPath
      .builder()
      .setUrl("https://downloads.mysql.com/docs/world-db.zip")
      .build();

    Path targetPath = Paths.get("target/world-db.zip");
    if (Files.exists(targetPath)) {
      Files.delete(targetPath);
    }
    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    long size = Files.size(targetPath);
    Assertions.assertTrue(size > 0, "Target File (" + targetPath + ") has a size (" + size + ") bigger than 0");
  }


  @Test
  public void testGetWithCopyRequest() throws IOException, URISyntaxException {
    URL website = new URL(httpBinUrl + "/html");
    Path sourcePath = Paths.get(website.toURI());
    Path targetPath = Paths.get("target/index.html");
    if (Files.exists(targetPath)) {
      Files.delete(targetPath);
    }
    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    long size = Files.size(targetPath);
    Assertions.assertTrue(size > 0, "Target File (" + targetPath + ") has a size (" + size + ") bigger than 0");
    Assertions.assertEquals(3741, size);
  }

  /**
   * {@link Files#readAllBytes(Path)}
   */
  @Test
  public void readAllBytes() throws IOException, URISyntaxException, InterruptedException {

    URL website = new URL(httpBinUrl + "/html");
    Path path = Paths.get(website.toURI());

    try (SeekableByteChannel sbc = Files.newByteChannel(path);
         InputStream in = Channels.newInputStream(sbc)) {
      Assertions.assertEquals(HttpSeekableByteChannel.class, sbc.getClass());
      HttpSeekableByteChannel sbcHttp = (HttpSeekableByteChannel) sbc;

      // The size should be known, not -1
      // because it's used by Java to create an array
      // to receive the bytes
      long size = sbcHttp.size();
      Assertions.assertNotEquals(-1, size);

    }

    String content = Fs.getFileContent(path);
    Assertions.assertTrue(content.contains("html"));

  }


  /**
   * Does not exist not yet fully implemented see:
   * See {@link HttpFileSystemProvider#checkAccess(Path, AccessMode...)}
   */
  @Disabled
  @Test
  public void doesNotExistDueTo401() throws MalformedURLException, URISyntaxException {

    URL website = new URL(httpBinUrl + "/status/401");
    Path sourcePath = Paths.get(website.toURI());
    boolean condition = Files.notExists(sourcePath);
    Assertions.assertTrue(condition);
    boolean readable = Files.isReadable(sourcePath);
    Assertions.assertFalse(readable);

  }

  @Test
  void mediaType() throws MalformedURLException, URISyntaxException, NotAbsoluteException {

    URL website = new URL(httpBinUrl + "/json");
    Path path = Paths.get(website.toURI());
    Assertions.assertEquals(MediaTypes.TEXT_JSON, Fs.detectMediaType(path));

  }
}
