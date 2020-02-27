package net.bytle.db.fs;

import net.bytle.db.spi.DataStoreProvider;

import java.util.Arrays;

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
   * Returns the URI scheme that identifies this provider.
   * TODO: get the accepted scheme dynamically from the NIOFS providers
   * @return true
   */
  @Override
  public boolean accept(String url) {
    String[] acceptedSchemes = {LOCAL_FILE_SCHEME, "http", "https"};
    return Arrays.stream(acceptedSchemes)
      .map(scheme->url.toLowerCase().startsWith(scheme))
      .filter(b->b)
      .findFirst()
      .orElse(false);
  }


}
