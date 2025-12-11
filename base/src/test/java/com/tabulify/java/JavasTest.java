package com.tabulify.java;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavasTest {

  @Test
  public void getFilePath() {
    Path path = Javas.getSourceCodePath(JavasTest.class);
    Assert.assertTrue(Files.exists(path));
  }

  @Test
  public void getFilePathFromUrl() throws MalformedURLException, URISyntaxException {

    /*
     * Code in a jar test
     */

    String uriString = "file:/D:/code/bytle-mono/db/build/libs/bytle-db-0.0.1-SNAPSHOT.jar";
    String urlString = "jar:" + uriString + "!/net/bytle/db/datastore/DatastoreVault.class";
    URL classFileUrl = new URL(urlString);
    Path expectedPath = Paths.get(URI.create(uriString));
    Path path = Javas.getFilePathFromUrl(classFileUrl);
    Assert.assertEquals("The jar path could be parsed",expectedPath.toString(),path.toString());


    urlString = "file:/D:/code/bytle-mono/db/build/classes/java/test/net/bytle/java/JavasTest.class";
    classFileUrl = new URL(urlString);
    expectedPath = Paths.get(classFileUrl.toURI());
    path = Javas.getFilePathFromUrl(classFileUrl);
    Assert.assertEquals("The class path could be parsed",expectedPath.toString(),path.toString());

  }
}
