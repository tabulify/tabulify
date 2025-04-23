package com.tabulify.mysql;

import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import net.bytle.type.Variable;

public class MySqlConnection extends SqlConnection {

  private MySqlMetadata mySqlMetadata;
  private MySqlDataSystem mySqlDataSystem;

  public MySqlConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);
  }


  @Override
  public MySqlMetadata getMetadata() {
    if (mySqlMetadata == null) {
      mySqlMetadata = new MySqlMetadata(this);
    }
    return mySqlMetadata;
  }

  @Override
  public SqlDataSystem getDataSystem() {
    if (mySqlDataSystem == null) {
      mySqlDataSystem = new MySqlDataSystem(this);
    }
    return mySqlDataSystem;
  }

  @Override
  public String getCurrentCatalog() {
    return null;
  }

  public boolean isPlanetScale(){
    String url = this.getURL();
    return url.contains("psdb.cloud");
  }

}
