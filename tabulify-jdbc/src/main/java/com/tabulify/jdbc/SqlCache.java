package com.tabulify.jdbc;

import com.tabulify.DbLoggers;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.tabulify.jdbc.SqlMediaType.SCRIPT;

/**
 * A cache
 * <p></p>
 * Note
 * * Due to foreign key, building of dag, building a data path may take a very long time
 * * Schema does not change as object
 * <p></p>
 * Object are added on creation and deleted on deletion.
 * If a property of an object such as a constraint, column is deleted,
 * the cache will not see it, you need to delete it on the in-memory object as wel as on the sql system
 * for consistency. If you don't want, you can drop it from the cache, but you also need to drop
 * any reference to your data path variable.
 */
public class SqlCache {


  private final Boolean builderCacheEnabled;

  public SqlCache(Boolean builderCacheEnabled) {
    this.builderCacheEnabled = builderCacheEnabled;
  }

  private final Map<String, SqlDataPath> sqlDataPathCache = new ConcurrentHashMap<>();

  public synchronized SqlDataPath createDataPath(String pathOrName, SqlMediaType sqlMediaType, Supplier<SqlDataPath> mappingFunction) {

    /**
     * Schema are always cached because they do not change as object
     * Schema is an object that may be asked during building
     * a lot of time. We don't build them each time
     */
    if (!builderCacheEnabled && sqlMediaType != SqlMediaType.SCHEMA) {
      return mappingFunction.get();
    }
    SqlDataPath sqlDataPath = sqlDataPathCache.get(pathOrName);
    if (sqlDataPath != null) {
      // schema may have an empty path, and we hit schema pretty often
      if (!pathOrName.isEmpty()) {
        DbLoggers.LOGGER_DB_ENGINE.finest("Cache Hit " + pathOrName);
      }
      return sqlDataPath;
    }

    if (!pathOrName.isEmpty()) {
      DbLoggers.LOGGER_DB_ENGINE.finest("Cache Added " + pathOrName);
    }
    SqlDataPath value = mappingFunction.get();
    sqlDataPathCache.put(pathOrName, value);
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
      if (sqlDataPath.getMediaType() == SCRIPT) {
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
    for (String key : sqlDataPathCache.keySet()) {
      sqlDataPathCache.remove(key);
    }
  }

  public Boolean inCache(SqlDataPath sqlDataPath) {
    return sqlDataPathCache.containsKey(sqlDataPath.getRelativePath());
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
