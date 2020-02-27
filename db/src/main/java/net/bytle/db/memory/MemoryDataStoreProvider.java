package net.bytle.db.memory;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataStoreProvider;

public class MemoryDataStoreProvider extends DataStoreProvider {

  public static final String SCHEME = "mem";
  static MemoryDataStoreProvider memoryDataStoreProvider;

  public static MemoryDataStoreProvider of() {
    if (memoryDataStoreProvider == null) {
      memoryDataStoreProvider = new MemoryDataStoreProvider();
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
  public DataStore createDataStore(String name, String url) {

    return new MemoryDataStore(name, url);

  }

  @Override
  public boolean accept(String url) {
    return url.toLowerCase().startsWith(SCHEME);
  }

}
