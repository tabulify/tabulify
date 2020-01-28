package net.bytle.db;

import net.bytle.db.uri.DataUri;

/**
 * Static {@link DataUri} functions
 */
public class DataUris {


  /**
   *
   * @param dataUri
   * @param path
   * @return a new data uri where only the path has changed
   */
  public static DataUri injectPath(DataUri dataUri, String path) {
    return DataUri.of(path+DataUri.AT_STRING+dataUri.getDataStore()+DataUri.QUESTION_MARK+dataUri.getQuery()+DataUri.HASH_TAG+dataUri.getFragment());
  }
}
