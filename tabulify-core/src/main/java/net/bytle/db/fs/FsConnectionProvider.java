package net.bytle.db.fs;

import net.bytle.db.Tabular;
import net.bytle.db.spi.ConnectionProvider;
import net.bytle.type.Variable;

import java.net.URI;
import java.nio.file.spi.FileSystemProvider;

public class FsConnectionProvider extends ConnectionProvider {



    private static FsConnectionProvider defaultTableSystemProvider;

    public static FsConnectionProvider getDefault() {
        if (defaultTableSystemProvider == null){
            defaultTableSystemProvider = new FsConnectionProvider();
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
    public FsConnection createConnection(Tabular tabular, Variable name, Variable uri) {

      return new FsConnection(tabular, name, uri);

    }

  /**
   *
   * @return true if there is a file system provider that takes into account his url
   */
  @Override
  public boolean accept(Variable url) {
    URI uri = URI.create((String) url.getValueOrDefaultOrNull());
    for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
      String scheme = uri.getScheme();
      if (scheme==null){
        // an URL for tpcds return null for scheme
        scheme = uri.getSchemeSpecificPart();
      }
      if (scheme.equals(fileSystemProvider.getScheme())) {
        return true;
      }
    }
    return false;
  }


}
