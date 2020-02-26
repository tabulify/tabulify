package net.bytle.db.tpc;

import net.bytle.db.spi.DataStoreProvider;

import java.util.Arrays;
import java.util.List;

public class TpcDataStoreProvider extends DataStoreProvider {


  public static final String TPCDS_SCHEME = "tpcds";
  static private TpcDataSetSystem tpcDataSetSystem = new TpcDataSetSystem();


  /**
   * Returns the URI scheme that identifies this provider.
   *
   * @return The URI scheme
   */
  @Override
  public List<String> getSchemes() {
    return Arrays.asList(TPCDS_SCHEME);
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
  public TpcDataSetSystem getDataStore() {

    return tpcDataSetSystem;

  }


}
