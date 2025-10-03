package com.tabulify.jdbc;

import com.tabulify.DbLoggers;
import net.bytle.exception.InternalException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache for perf
 * <p></p>
 * Why?
 * * Build time of a data path may take a very long time due:
 * * to foreign key (we need the foreign constraint and the columns),
 * * building of dag to get a `create/drop` order or just dependencies
 * * Schema does not change as object and are used quiet heavily
 * <p></p>
 * Note:
 * * Object are:
 * * added on creation
 * * and deleted on deletion.
 * * If a property of an object such as a constraint, column is deleted, the cache will not see it,
 * you need to delete it on the in-memory object as wel as on the sql system
 * for consistency.
 */
public class SqlCache {


  private final Boolean builderCacheEnabled;

  public SqlCache(Boolean builderCacheEnabled) {
    this.builderCacheEnabled = builderCacheEnabled;
  }

  /**
   * Relative Path is the string identifier
   */
  private Map<String, SqlDataPath> sqlDataPathCache = new HashMap<>();

  public synchronized SqlDataPath createDataPath(String relativePath, SqlMediaType sqlMediaType, Supplier<SqlDataPath> mappingFunction) {


    /**
     * Schema are always cached because they do not change as object
     * Schema is an object that may be asked during building
     * a lot of time. We don't build them each time
     */
    if (!builderCacheEnabled && sqlMediaType != SqlMediaType.SCHEMA) {
      return mappingFunction.get();
    }

    /**
     * Check
     */
    if (sqlMediaType.isRuntime()) {
      throw new InternalException("This should not be a script as this is the signature for an non-script data resource");
    }

    SqlDataPath sqlDataPath = sqlDataPathCache.get(relativePath);
    if (sqlDataPath != null) {
      // schema may have an empty path, and we hit schema pretty often
      if (!relativePath.isEmpty()) {
        DbLoggers.LOGGER_DB_ENGINE.finest("Cache Hit " + relativePath);
      }
      return sqlDataPath;
    }

    if (!relativePath.isEmpty()) {
      DbLoggers.LOGGER_DB_ENGINE.finest("Cache Added " + relativePath);
    }
    SqlDataPath value = mappingFunction.get();
    sqlDataPathCache.put(relativePath, value);
    return value;

  }

  private SqlCache drop(SqlDataPath sqlDataPath, Boolean strict) {


    String nameToDelete = null;
    for (Map.Entry<String, SqlDataPath> entry : sqlDataPathCache.entrySet()) {
      if (sqlDataPath.equals(entry.getValue())) {
        nameToDelete = entry.getKey();
        break;
      }
    }
    if (nameToDelete == null) {
      if (sqlDataPath.isRuntime()) {
        // not cached
        return this;
      }
      if (!this.builderCacheEnabled || !strict) {
        DbLoggers.LOGGER_DB_ENGINE.finest("Cache Delete Miss " + sqlDataPath);
        return this;
      }
      throw new RuntimeException("SqlCache: SqlDataPath " + sqlDataPath + " not found in the cache, but it should have been build first");
    }
    DbLoggers.LOGGER_DB_ENGINE.finest("Cache Delete " + nameToDelete);
    sqlDataPathCache.remove(nameToDelete);
    return this;
  }

  public SqlCache drop(SqlDataPath sqlDataPath) {
    drop(sqlDataPath, true);
    return this;
  }

  public void empty() {
    sqlDataPathCache = new HashMap<>();
  }

  public Boolean inCache(SqlDataPath sqlDataPath) {
    return sqlDataPathCache.containsKey(sqlDataPath.getCompactPath());
  }

  public void dropIfExist(SqlDataPath dataPath) {
    drop(dataPath, false);
  }

  public int size() {
    return sqlDataPathCache.size();
  }

  public Collection<SqlDataPath> getDataPaths() {
    return sqlDataPathCache.values();
  }

  /**
   * When creating test, data path may be `drop` (in db and cache) and
   * held in memory, this function is used in the `create` statement
   * to put back in the data path
   */
  public void addIfNotPresent(SqlDataPath sqlDataPath) {
    sqlDataPathCache.put(sqlDataPath.getName(), sqlDataPath);
  }

  public boolean isEmpty() {
    return sqlDataPathCache.isEmpty();
  }
}
