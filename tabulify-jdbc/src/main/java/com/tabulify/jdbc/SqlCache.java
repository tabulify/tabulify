package com.tabulify.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache
 * note
 * * Due to foreign key, building of dag, building a data path may take a very long time
 * * Schema does not change as object
 */
public class SqlCache {


  private final Boolean builderCacheEnabled;

  public SqlCache(Boolean builderCacheEnabled) {
    this.builderCacheEnabled = builderCacheEnabled;
  }


  private final Map<String, SqlDataPath> sqlDataPathCache = new HashMap<>();

  public SqlDataPath createDataPath(String pathOrName, SqlMediaTypeType sqlMediaType, Supplier<SqlDataPath> mappingFunction) {

    /**
     * Schema are always cached because they do not change as object
     * Schema is an object that may be asked during building
     * a lot of time. We don't build them each time
     */
    if (!builderCacheEnabled && sqlMediaType != SqlMediaTypeType.SCHEMA) {
      return mappingFunction.get();
    }


    return sqlDataPathCache.computeIfAbsent(pathOrName, k -> mappingFunction.get());
  }

  public SqlCache drop(SqlDataPath sqlDataPath) {
    String nameToDelete = null;
    for (Map.Entry<String, SqlDataPath> entry : sqlDataPathCache.entrySet()) {
      if (sqlDataPath.equals(entry.getValue())) {
        nameToDelete = entry.getKey();
        break;
      }
    }
    if (nameToDelete == null) {
      throw new RuntimeException("SqlDataPath " + sqlDataPath + " not found in the cache, but it should have been build first");
    }
    sqlDataPathCache.remove(nameToDelete);
    return this;
  }
}
