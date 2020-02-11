package net.bytle.db.jdbc;

import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;

import java.util.Arrays;
import java.util.List;

public class JdbcDataSystemProvider extends TableSystemProvider {

  static {
    jdbcDataSystem = new JdbcDataSystem();
  }

  static protected JdbcDataSystem jdbcDataSystem;

  public static final String JDBC_SCHEME = "jdbc";

  /**
   * Returns the URI scheme that identifies this provider.
   *
   * @return The URI scheme
   */
  @Override
  public List<String> getSchemes() {
    return Arrays.asList(JDBC_SCHEME);
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
  public TableSystem getTableSystem() {

    return jdbcDataSystem;

  }

}
