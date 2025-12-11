package com.tabulify.type;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

class JdbcUriTest {

  @Test
  void sqlite() throws URISyntaxException {

    // Not sure if this is valid on windows
    // String url = "jdbc:sqlite:///C:\\\\Users\\\\gerard\\\\AppData\\\\Local\\\\Temp\\\\bytle-db\\\\defaultDb.db";

    String uriString = "jdbc:sqlite:///C:/Users/defaultDb.db";
    URI uri = new URI(uriString);
    JdbcUri jdbcUri = new JdbcUri(uri);
    Assertions.assertEquals(uriString, jdbcUri.toString());
    Assertions.assertEquals("jdbc", jdbcUri.getScheme());
    Assertions.assertEquals("sqlite", jdbcUri.getSqlScheme());
  }

  @Test
  void oracle() throws URISyntaxException {

    String uriString = "jdbc:oracle:thin:@localhost:1521/freepdb1";
    URI uri = new URI(uriString);
    JdbcUri jdbcUri = new JdbcUri(uri);
    Assertions.assertEquals(uriString, jdbcUri.toString());
    Assertions.assertEquals("jdbc", jdbcUri.getScheme());
    Assertions.assertEquals("oracle", jdbcUri.getSqlScheme());
    Assertions.assertEquals("localhost", jdbcUri.getHost().toString());
    Assertions.assertEquals(1521, jdbcUri.getPort());
    Assertions.assertEquals("/freepdb1", jdbcUri.getPath());

  }

  @Test
  void sap() throws URISyntaxException {

    String uriString = "jdbc:sap://linuxhana:30015/database?user=login&password=1234";
    URI uri = new URI(uriString);
    JdbcUri jdbcUri = new JdbcUri(uri);
    Assertions.assertEquals(uriString, jdbcUri.toString());
    Assertions.assertEquals("jdbc", jdbcUri.getScheme());
    Assertions.assertEquals("sap", jdbcUri.getSqlScheme());
    Assertions.assertEquals("linuxhana", jdbcUri.getHost().toString());
    Assertions.assertEquals(30015, jdbcUri.getPort());

  }

  @Test
  void sqlServer() throws URISyntaxException {
    String uriString = "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;encrypt=true;trustServerCertificate=true";
    URI uri = new URI(uriString);
    JdbcUri jdbcUri = new JdbcUri(uri);
    Assertions.assertEquals(uriString, jdbcUri.toString());
    Assertions.assertEquals("jdbc", jdbcUri.getScheme());
    Assertions.assertEquals("sqlserver", jdbcUri.getSqlScheme());
    Assertions.assertEquals("localhost", jdbcUri.getHost().toString());
    Assertions.assertEquals(1433, jdbcUri.getPort());
  }

}

