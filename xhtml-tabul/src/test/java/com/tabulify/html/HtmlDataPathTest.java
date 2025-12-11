package com.tabulify.html;

import com.tabulify.Tabular;
import com.tabulify.fs.FsConnection;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.test.SingletonTestContainer;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferResourceOperations;
import com.tabulify.uri.DataUriNode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class HtmlDataPathTest {

  private String httpBinUrl;
  private String httpBinUrlResources;
  private Tabular tabular;
  private FsConnection resourceDataStore;

  @BeforeEach
  void setUp() {
    tabular = Tabular.tabularWithCleanEnvironment();

    if (this.httpBinUrl == null) {
      resourceDataStore = tabular.createRuntimeConnectionForResources(HtmlDataPathTest.class, "html");
      Path path = resourceDataStore.getNioPath();
      // A unique port, we start with 81 so that we don't conflict with 8081 port
      // A software had reserved the range `8082 to 8181`, pfff
      // Traditional development ports: 3000-3999
      int hostPort = 8051;
      SingletonTestContainer testContainerWrapper = new SingletonTestContainer("httpbin", "kennethreitz/httpbin:latest")
        .withPort(hostPort, 80)
        .withBindMount(path.getParent(), "/usr/local/lib/python3.6/dist-packages/httpbin/static")
        .startContainer();
      this.httpBinUrl = "http://" + testContainerWrapper.getHostName() + ":" + testContainerWrapper.getHostPort();
      this.httpBinUrlResources = this.httpBinUrl + "/static";

    }


  }

  @Test
  public void countryTableAccessFileDirectory() {


    FsConnection connection = tabular.createRuntimeConnectionForResources(HtmlDataPathTest.class, "html");
    HtmlDataPath htmlDataPath = (HtmlDataPath) connection.getDataPath("WikipediaCountriesTable.html");

    Assertions.assertTrue(htmlDataPath.getSize() > 40000);
    Assertions.assertEquals(Long.valueOf(242), htmlDataPath.getCount());
    RelationDef relationDef = htmlDataPath.getOrCreateRelationDef();
    Assertions.assertEquals(8, relationDef.getColumnsSize());
    Tabulars.print(htmlDataPath);


  }

  @Test
  public void countryTableGetByUrl() {


    HtmlDataPath htmlDataPath = (HtmlDataPath) tabular.getDataPath(httpBinUrlResources + "/html/WikipediaCountriesTable.html");

    Assertions.assertTrue(htmlDataPath.getCount() > 200);
    Assertions.assertTrue(htmlDataPath.getSize() > 40000);
    RelationDef relationDef = htmlDataPath.getOrCreateRelationDef();
    Assertions.assertEquals(8, relationDef.getColumnsSize());
    Tabulars.print(htmlDataPath);


  }


  @Test
  public void countryTableSelectByUrl() {


    String spec = httpBinUrlResources + "/html/WikipediaCountriesTable.html";
    DataUriNode dataUriSelector = tabular.createDataUri(spec);
    Assertions.assertEquals("http", dataUriSelector.getConnection().getScheme());
    List<DataPath> htmlDataPaths = tabular.select(dataUriSelector);
    Assertions.assertEquals(1, htmlDataPaths.size());
    DataPath htmlDataPath = htmlDataPaths.get(0);
    Assertions.assertTrue(htmlDataPath.getSize() > 40000, "Size is " + htmlDataPath.getSize());
    Assertions.assertTrue(200L < htmlDataPath.getCount());
    RelationDef relationDef = htmlDataPath.getOrCreateRelationDef();
    Assertions.assertTrue(4 < relationDef.getColumnsSize());
    Tabulars.print(htmlDataPath);


  }

  @Test
  public void copyFileWithoutAnyTableTest() throws IOException, URISyntaxException {


    URL website = new URL(this.httpBinUrl + "/html");
    Path sourcePath = Paths.get(website.toURI());
    DataPath sourceDataPath = tabular.getDataPath(sourcePath);
    Path tempPath = Files.createTempFile("html", ".html");
    if (Files.exists(tempPath)) {
      Files.delete(tempPath);
    }
    DataPath targetDataPath = tabular.getDataPath(tempPath);
    TransferListener transferListener = Tabulars.copy(sourceDataPath, targetDataPath, TransferResourceOperations.DROP);
    Assertions.assertEquals(0, transferListener.getExitStatus(), "Exit status was good");
    long size = Files.size(tempPath);
    Assertions.assertTrue(size > 0, "Target File (" + tempPath + ") has a size (" + size + ") bigger than 0");


  }

}
