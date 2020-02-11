package net.bytle.db.oracle;

import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataStoreExtensionProvider;

public class OraDataStoreExtensionProvider extends JdbcDataStoreExtensionProvider {

  @Override
  public String getProductName() {
    return "oracle";
  }

  @Override
  public JdbcDataStoreExtension getJdbcDataStoreExtension(JdbcDataStore jdbcDataStore) {
    return new OraDataStoreExtension(jdbcDataStore);
  }

}
