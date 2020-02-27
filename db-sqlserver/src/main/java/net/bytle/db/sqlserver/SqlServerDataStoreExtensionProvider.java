package net.bytle.db.sqlserver;

import net.bytle.db.jdbc.SqlDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataStoreExtensionProvider;

public class SqlServerDataStoreExtensionProvider extends JdbcDataStoreExtensionProvider {

  @Override
  public String getProductName() {
    return "Microsoft SQL Server";
  }

  @Override
  public JdbcDataStoreExtension getJdbcDataStoreExtension(SqlDataStore jdbcDataStore) {
    return new SqlServerDataStoreExtension(jdbcDataStore);
  }


}
