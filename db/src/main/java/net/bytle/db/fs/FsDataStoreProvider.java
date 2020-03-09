package net.bytle.db.fs;

import net.bytle.db.spi.DataStoreProvider;

import java.net.URI;
import java.nio.file.spi.FileSystemProvider;

public class FsDataStoreProvider extends DataStoreProvider {


    public static final String LOCAL_FILE_SCHEME = "file";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTP_SCHEMES = "https";


    private static FsDataStoreProvider defaultTableSystemProvider;

    public static FsDataStoreProvider getDefault() {
        if (defaultTableSystemProvider == null){
            defaultTableSystemProvider = new FsDataStoreProvider();
        }
        return defaultTableSystemProvider;
    }

    /**
     * Returns an existing {@code work} created by this provider.
     *
     * <br>
     * If a security manager is installed then a provider implementation
     * may require to check a permission before returning a reference to an
     * existing work.
     *
     * @return The table system
     * @throws SecurityException If a security manager is installed and it denies an unspecified
     *                           permission.
     */
    @Override
    public FsDataStore createDataStore(String name, String url) {

      return new FsDataStore(name, url);

    }

  /**
   *
   * @return true if there is a file system provider that takes into account his url
   */
  @Override
  public boolean accept(String url) {
    URI uri = URI.create(url);
    for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
      if (uri.getScheme().equals(fileSystemProvider.getScheme())) {
        return true;
      }
    }
    return false;
  }


}
