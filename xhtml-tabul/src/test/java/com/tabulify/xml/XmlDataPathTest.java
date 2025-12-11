package com.tabulify.xml;

import com.tabulify.Tabular;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.IllegalStructure;
import com.tabulify.exception.NotAbsoluteException;
import com.tabulify.fs.Fs;
import com.tabulify.niofs.http.HttpPath;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;
import com.tabulify.type.UriEnhanced;
import com.tabulify.type.time.DurationShort;
import com.tabulify.type.time.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;

public class XmlDataPathTest {


  @Test
  public void base() throws SelectException {
    try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {
      FsConnection resourceConnection = tabular.createRuntimeConnectionForResources(XmlDataPathTest.class, "wikipedia");
      FsDataPath dataPath = (FsDataPath) resourceConnection.getDataPath("bar.xml");
      Assertions.assertInstanceOf(XmlDataPath.class, dataPath);
      Assertions.assertEquals(1, dataPath.getOrCreateRelationDef().getColumnsSize());
      ColumnDef<?> columnDef = dataPath.getOrCreateRelationDef().getColumnDef(1);
      Assertions.assertEquals(XmlDataPath.XML_DEFAULT_HEADER_NAME, columnDef.getColumnName());
      Assertions.assertEquals(Types.SQLXML, columnDef.getDataType().getVendorTypeNumber());
      Assertions.assertEquals(1L, (long) dataPath.getCount());
      Tabulars.print(dataPath);
      try (SelectStream selectStream = dataPath.getSelectStream()) {
        selectStream.next();
        String output = selectStream.getString(1);
        Assertions.assertEquals("<?xml version=\"1.1\"?>\n<bar>foo</bar>\n", output);
      }
    }
  }

  @Test
  public void httpXml() throws NotAbsoluteException, IllegalStructure {

    String url = "https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=xml&prop=description|categories";
    // do we have an HttpPath?
    Path path = Paths.get(UriEnhanced.createFromString(url).toUri());
    Assertions.assertEquals(HttpPath.class, path.getClass());
    MediaType mediaType = Fs.detectMediaType(path);
    Assertions.assertEquals(MediaTypes.TEXT_XML, mediaType);

    try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {

      Timer timer = Timer.create("load").start();
      DataPath dataPath = tabular.getDataPath(url);
      Assertions.assertEquals(dataPath.getClass().getSimpleName(), XmlDataPath.class.getSimpleName());
      timer.stop();
      System.out.println(DurationShort.create(timer.getDuration()).toString());

    }
  }
}
