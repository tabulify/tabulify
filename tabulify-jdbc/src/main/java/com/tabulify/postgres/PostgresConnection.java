package com.tabulify.postgres;

import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;
import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.conf.Attribute;
import net.bytle.type.MediaType;

public class PostgresConnection extends SqlConnection {

  public PostgresConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }


  @Override
  public PostgresDataSystem getDataSystem() {
    return new PostgresDataSystem(this);
  }


  @Override
  public SqlDataPath getDataPath(String path, MediaType mediaType) {

    return new PostgresDataPath(this, path, mediaType);

  }

  @Override
  public SqlConnectionMetadata getMetadata() {
    return new PostgresFeatures(this);
  }
}
