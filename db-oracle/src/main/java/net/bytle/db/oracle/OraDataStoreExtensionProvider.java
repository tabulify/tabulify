package net.bytle.db.oracle;

import net.bytle.db.jdbc.AnsiDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataStoreExtensionProvider;

public class OraDataStoreExtensionProvider extends JdbcDataStoreExtensionProvider {

  @Override
  public String getProductName() {
    return "Oracle";
  }

  @Override
  public JdbcDataStoreExtension getJdbcDataStoreExtension(AnsiDataStore jdbcDataStore) {
    return new OraDataStoreExtension(jdbcDataStore);
  }

}
