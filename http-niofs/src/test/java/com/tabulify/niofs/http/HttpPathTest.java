package com.tabulify.niofs.http;

import com.tabulify.niofs.http.HttpFileSystem;
import com.tabulify.niofs.http.HttpFileSystemProvider;
import com.tabulify.niofs.http.HttpPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;


public class HttpPathTest {


    @Test
    public void getParent() throws MalformedURLException, URISyntaxException {

        URL website = new URL("https://example.com/foo/bar");
        Path sourcePath = Paths.get(website.toURI());
        Assertions.assertEquals("bar", sourcePath.getFileName().toString(), "File name");
        Assertions.assertTrue(sourcePath.isAbsolute(), "Path is absolute");

        // parent
        Path parent = sourcePath.getParent();
        Assertions.assertEquals("/foo", parent.toString(), "Parent name");
        Assertions.assertEquals("foo", parent.getFileName().toString(), "Parent name");
        Assertions.assertEquals("https://example.com/foo", parent.toUri().toString(), "Parent name");


    }

    @Test
    public void getSibling() throws MalformedURLException, URISyntaxException {
        URL website = new URL("https://example.com/foo/bar");
        Path sourcePath = Paths.get(website.toURI());
        Assertions.assertEquals("bar", sourcePath.getFileName().toString(), "File name");
        Assertions.assertTrue(sourcePath.isAbsolute(), "Path is absolute");

        // parent
        Path parent = sourcePath.getParent();
        Assertions.assertEquals("/foo", parent.toString());
        Assertions.assertEquals("foo", parent.getFileName().toString());
        Assertions.assertEquals("https://example.com/foo", parent.toUri().toString());

        // sibling
        Path sibling = sourcePath.resolveSibling("sibling");
        Assertions.assertEquals("/foo/sibling", sibling.toString(), "Sibling path");
        Assertions.assertEquals("sibling", sibling.getFileName().toString(), "Sibling name");
        Assertions.assertEquals("https://example.com/foo/sibling", sibling.toUri().toString(), "Sibling URI name");
        Assertions.assertEquals("/", sibling.getRoot().toString(), "Sibling Root");

    }

    @Test
    public void getParentNull() throws MalformedURLException, URISyntaxException {
        URL website = new URL("https://example.com/");
        Path sourcePath = Paths.get(website.toURI());

        Assertions.assertEquals("/", sourcePath.toString());

        // the spec say to it must be null
        Path parent = sourcePath.getParent();
        Assertions.assertNull(parent);
    }

    @Test
    public void getHttpSystemFromUriWithoutPath() throws MalformedURLException, URISyntaxException {

        URL website = new URL("https://example.com");
        Path path = Paths.get(website.toURI());

        Assertions.assertEquals("", path.toString());
        Assertions.assertFalse(path.isAbsolute());
        Assertions.assertEquals("https://example.com", path.toUri().toString());

        path = path.resolve("yolo");
        Assertions.assertFalse(path.isAbsolute());
        path = path.toAbsolutePath();
        Assertions.assertEquals("/yolo", path.toString());

        Assertions.assertEquals("https://example.com/yolo", path.toUri().toString());

    }



    /**
     * Root without any path
     */
    @Test
    void testUrlWithoutPath() {

        String urlString = "https://example.com";
        URI website = URI.create(urlString);
        HttpFileSystemProvider provider = new HttpFileSystemProvider();
        try (HttpFileSystem httpFileSystem = provider.newFileSystem(website, new HashMap<>())) {
            Path path = httpFileSystem.getPath(website.getPath());
            // Always absolute the disk is https://example.com and the path /
            Assertions.assertFalse(path.isAbsolute());
            Path absolutePath = path.toAbsolutePath();
            Assertions.assertTrue(absolutePath.isAbsolute());
            Assertions.assertEquals("/", absolutePath.toString());
        }

    }

    @Test
    public void testAbsolutePath() {


        String urlString = "https://example.com";
        URI website = URI.create(urlString);
        HttpFileSystemProvider provider = new HttpFileSystemProvider();
        try (HttpFileSystem httpFileSystem = provider.newFileSystem(website, new HashMap<>())) {

            Path path = httpFileSystem.getPath("/yolo");
            Assertions.assertTrue(path.isAbsolute());

            path = httpFileSystem.getPath("/yolo", "yolo");
            Assertions.assertTrue(path.isAbsolute());
            Assertions.assertEquals("/yolo/yolo", path.toString());

        }


    }

    @Test
    public void relativePath() throws IOException, URISyntaxException {

        URL website = new URL("https://datacadamia.com/start");
        try (HttpFileSystem fileSystem = (HttpFileSystem) FileSystems.newFileSystem(website.toURI(), new HashMap<>())) {

            Path path = fileSystem.getPath("yolo");
            Assertions.assertFalse(path.isAbsolute());

            path = path.toAbsolutePath();
            Assertions.assertEquals("/start/yolo", path.toString());

            Path parent = path.getParent();
            Assertions.assertEquals("/start", parent.toString());

            Assertions.assertEquals(fileSystem.getWorkingStringPath(), parent.toString());

            path = fileSystem.getPath("foo", "bar");
            Assertions.assertFalse(path.isAbsolute());
            Assertions.assertEquals("foo/bar", path.toString());

            path = fileSystem.getPath("/foo/bar");
            Path root = path.getRoot();
            Assertions.assertNotNull(root);
            Path relativePathFromRoot = root.relativize(path);
            Assertions.assertEquals("foo/bar", relativePathFromRoot.toString());
        }

    }

  @Test
  public void relativize() throws IOException, URISyntaxException {

    URL website = new URL("https://datacadamia.com/");
    try (HttpFileSystem fileSystem = (HttpFileSystem) FileSystems.newFileSystem(website.toURI(), new HashMap<>())) {

      HttpPath path = fileSystem.getPath("/").relativize(Paths.get("/start"));
      Assertions.assertEquals("start",path.toString());
      Assertions.assertEquals("https://datacadamia.com/start",path.toUri().toString());

    }

    website = new URL("https://datacadamia.com/start");
    try (HttpFileSystem fileSystem = (HttpFileSystem) FileSystems.newFileSystem(website.toURI(), new HashMap<>())) {

      HttpPath path = fileSystem.getPath("start");
      Assertions.assertEquals("https://datacadamia.com/start/start",path.toUri().toString());

    }

  }

  @Test
  public void toURI() throws IOException, URISyntaxException {

    URL website = new URL("https://datacadamia.com/start");
    try (HttpFileSystem fileSystem = (HttpFileSystem) FileSystems.newFileSystem(website.toURI(), new HashMap<>())) {

      String path = fileSystem.getPath("/").toUri().toString();
      Assertions.assertEquals("https://datacadamia.com",path);

      path = fileSystem.getPath("/start").toUri().toString();
      Assertions.assertEquals("https://datacadamia.com/start",path);

    }

  }


    @Test
    void resolve() throws IOException, URISyntaxException {

        URL website = new URL("https://datacadamia.com/start");
        try (HttpFileSystem fileSystem = (HttpFileSystem) FileSystems.newFileSystem(website.toURI(), new HashMap<>())) {

            Path thisPath = fileSystem.getPath("");
            Path other = fileSystem.getPath("foo").toAbsolutePath();
            Assertions.assertEquals(other,thisPath.resolve(other),"when other is absolute, resolve return other");

            other = fileSystem.getPath("");
            Assertions.assertEquals(thisPath,thisPath.resolve(other),"when other is empty path, resolve return this");

        }

    }
}
