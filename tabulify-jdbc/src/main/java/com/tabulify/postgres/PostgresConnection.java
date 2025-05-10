package com.tabulify.postgres;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;
import com.tabulify.jdbc.SqlDataPath;
import net.bytle.type.MediaType;

import java.util.function.Supplier;

public class PostgresConnection extends SqlConnection {

  public PostgresConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }


  @Override
  public PostgresDataSystem getDataSystem() {
    return new PostgresDataSystem(this);
  }


  @Override
  protected Supplier<SqlDataPath> getDataPathSupplier(String pathOrName, MediaType mediaType) {
    return () -> new PostgresDataPath(this, pathOrName, mediaType);
  }

  @Override
  public SqlConnectionMetadata getMetadata() {
    return new PostgresFeatures(this);
  }

}
