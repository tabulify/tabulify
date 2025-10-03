package com.tabulify.model;

import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.sql.SQLType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The types of a vendor (ie database).
 * Vendor comes from {@link SQLType#getVendor()}
 * This is the type that we define during the creation of a system module
 * <p></p>
 * The main advantage between a JDBC data type and the Vendor is that it defines the class to use
 * It's not given by the driver and the type code does not correspond one on one with a class
 * For instance, Postgres returns a {@link java.sql.Types#TIMESTAMP} for a {@link java.sql.Types#TIMESTAMP_WITH_TIMEZONE} type
 */
public interface SqlDataTypeVendor extends SqlDataTypeKeyInterface {

  Class<?> getValueClass();

  int getMaxPrecision();

  int getMaximumScale();

  SqlDataTypeAnsi getAnsiType();

  String getDescription();

  /**
   * @return name aliases
   * KeyInterface can hold a {@link SqlDataTypeAnsi} and a name
   */
  List<KeyInterface> getAliases();


  default List<KeyNormalizer> getNormalizedAliases() {
    return getAliases().stream().map(KeyInterface::toKeyNormalizer).collect(Collectors.toList());
  }

  /**
   * @return the minimum scale
   */
  default int getMinScale() {
    return 0;
  }

  /**
   * @return the default precision if not set
   */
  default int getDefaultPrecision() {
    return 0;
  }

}
