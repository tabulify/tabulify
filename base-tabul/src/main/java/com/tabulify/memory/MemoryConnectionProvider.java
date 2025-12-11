package com.tabulify.memory;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.spi.ConnectionProvider;
import com.tabulify.type.KeyNormalizer;

public class MemoryConnectionProvider extends ConnectionProvider {


  public static final KeyNormalizer SCHEME = KeyNormalizer.createSafe("mem");
  public static final String URI = SCHEME + ":/";

  static MemoryConnectionProvider memoryDataStoreProvider;

  public static MemoryConnectionProvider of() {
    if (memoryDataStoreProvider == null) {
      memoryDataStoreProvider = new MemoryConnectionProvider();
    }
    return memoryDataStoreProvider;
  }


  /**
   * Returns an existing {@code work} created by this provider.
   * <p/>
   * The work is identified by its {@code URI}. Its exact form
   * is highly provider dependent.
   * <p/>
   * <p> If a security manager is installed then a provider implementation
   * may require to check a permission before returning a reference to an
   * existing work.
   *
   * @return The sql database
   * @throws SecurityException If a security manager is installed and it denies an unspecified
   *                           permission.
   */
  @Override
  public Connection createConnection(Tabular tabular, Attribute name, Attribute url) {

    return new MemoryConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().toLowerCase().startsWith(SCHEME.toString());
  }


}
