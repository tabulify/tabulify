package net.bytle.db.noop;

import net.bytle.db.Tabular;
import net.bytle.db.spi.ConnectionProvider;
import net.bytle.type.Variable;

public class NoopConnectionProvider extends ConnectionProvider {


  public static final String NOOP_SCHEME = "noop";
  private static NoopConnectionProvider defaultTableSystemProvider;


  public static NoopConnectionProvider getDefault() {
    if (defaultTableSystemProvider == null) {
      defaultTableSystemProvider = new NoopConnectionProvider();
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
  public NoOpConnection createConnection(Tabular tabular, Variable name, Variable uri) {

    return new NoOpConnection(tabular, name, uri);

  }

  /**
   * @return true if there is a file system provider that takes into account his url
   */
  @Override
  public boolean accept(Variable uri) {
    return uri.getValueOrDefaultAsStringNotNull().equals(NOOP_SCHEME);
  }


}
