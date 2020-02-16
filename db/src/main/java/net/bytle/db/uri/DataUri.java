package net.bytle.db.uri;

import java.util.Map;

/**
 * A data uri is the string representation of a data path
 */
public class DataUri implements Comparable<DataUri> {

  public static final String QUESTION_MARK = "?";
  public static final String HASH_TAG = "#";
  public static final String AT_STRING = "@";

  private final String uri ;
  private String query = null;
  private String fragment = null;
  private String path;
  private String dataStore;


  private DataUri(String uri) {
    assert uri != null : "The data uri given is null. If you want to build a data uri, see function of()";

    this.uri = uri;

    int atIndex = uri.indexOf(AT_STRING);
    if (atIndex == -1) {
      throw new RuntimeException("The at (@) string is mandatory in a data uri and was not found");
    }
    if (atIndex != 0) {
      path = uri.substring(0, atIndex);
    }

    // Data Store parsing
    int questionMarkIndex = uri.indexOf(QUESTION_MARK);
    if (questionMarkIndex != -1) {
      dataStore = uri.substring(atIndex + 1, questionMarkIndex);
    }
    int hashTagIndex = uri.indexOf(HASH_TAG);
    if (dataStore == null && hashTagIndex != -1) {
      dataStore = uri.substring(atIndex + 1, hashTagIndex);
    }

    if (dataStore == null) {
      dataStore = uri.substring(atIndex + 1);
    }

    if (dataStore.equals("")) {
      throw new RuntimeException("The data store name cannot be null");
    }

    // Query
    if (questionMarkIndex != -1) {
      if (hashTagIndex == -1) {
        query = uri.substring(questionMarkIndex + 1);
      } else {
        query = uri.substring(questionMarkIndex + 1, hashTagIndex);
      }
    }

    // Fragment
    if (hashTagIndex != -1) {
      fragment = uri.substring(hashTagIndex + 1);
    }

  }

  // Private
  private DataUri() {
    this.uri = null;
  }


  public static DataUri of(String uri) {
    assert uri != null : "The uri should not be null";
    return new DataUri(uri);
  }

  public static DataUri of() {
    return new DataUri();
  }


  public String getPath() {
    return this.path;
  }


  public String toString() {
    // We miss the query and fragment parts but they are actually not used
    if (path==null) {
      return AT_STRING + dataStore;
    } else {
      return path + AT_STRING + dataStore;
    }
  }


  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
   * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
   * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
   * <tt>y.compareTo(x)</tt> throws an exception.)
   *
   * <p>The implementor must also ensure that the relation is transitive:
   * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
   * <tt>x.compareTo(z)&gt;0</tt>.
   *
   * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
   * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
   * all <tt>z</tt>.
   *
   * <p>It is strongly recommended, but <i>not</i> strictly required that
   * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
   * class that implements the <tt>Comparable</tt> interface and violates
   * this condition should clearly indicate this fact.  The recommended
   * language is "Note: this class has a natural ordering that is
   * inconsistent with equals."
   *
   * <p>In the foregoing description, the notation
   * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
   * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
   * <tt>0</tt>, or <tt>1</tt> according to whether the value of
   * <i>expression</i> is negative, zero or positive.
   *
   * @param o the object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   * is less than, equal to, or greater than the specified object.
   * @throws NullPointerException if the specified object is null
   * @throws ClassCastException   if the specified object's type prevents it
   *                              from being compared to this object.
   */
  @Override
  public int compareTo(DataUri o) {
    return this.toString().compareTo(o.toString());
  }

  public String getFragment() {
    return fragment;
  }

  public String getQuery() {
    return query;
  }

  public String getDataStore() {
    return dataStore;
  }

  public Map<String, String> getQueryParameters() {

    return Uris.getQueryAsMap(query);

  }

  public DataUri setPath(String path) {
    this.path = path;
    return this;
  }

  public DataUri setDataStore(String name) {
    this.dataStore = name;
    return this;
  }
}
