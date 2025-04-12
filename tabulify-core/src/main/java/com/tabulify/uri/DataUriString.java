package com.tabulify.uri;

import com.tabulify.connection.Connection;
import net.bytle.regexp.Glob;

import java.util.Objects;

/**
 * A data uri is the string representation of a data path
 */
public class DataUriString implements Comparable<DataUriString> {


  /**
   * The data store separator
   */
  public static final char AT_CHAR = '@';

  /**
   * When a datastore or a path is specified
   * by a data uri
   */
  public static final char BLOCK_OPEN = '(';
  public static final char BLOCK_CLOSE = ')';

  private final String uri;

  private String path;
  private String dataStore;


  private DataUriString(String uri) {

    assert uri != null : "The data uri given is null. If you want to build a data uri, see function of()";
    assert !uri.equals("") : "The data uri should not be the empty string";

    this.uri = uri;

    boolean blockStarted = false;
    StringBuilder stringBuilder = new StringBuilder();
    boolean dataStorePart = false;
    for (int i = 0; i < uri.length(); i++) {
      char c = uri.charAt(i);
      switch (c) {
        case BLOCK_OPEN:
        case BLOCK_CLOSE:
          //noinspection RedundantIfStatement
          if (c == BLOCK_OPEN) {
            blockStarted = true;
          } else {
            blockStarted = false;
          }
          stringBuilder.append(c);
          break;
        case AT_CHAR:
          if (!blockStarted) {
            if (stringBuilder.length() > 0) {
              path = stringBuilder.toString();
            } else {
              path = null;
            }
            dataStorePart = true;
            stringBuilder = new StringBuilder();
          } else {
            stringBuilder.append(c);
          }
          break;
        default:
          stringBuilder.append(c);
      }
    }

    /**
     * Last part
     */
    if (stringBuilder.length() > 0) {
      if (!dataStorePart) {
        path = stringBuilder.toString();
      } else {
        dataStore = stringBuilder.toString();
      }
    }


  }

  // Private
  private DataUriString() {
    this.uri = null;
  }


  public static DataUriString createFromString(String uri) {
    assert uri != null;
    /**
     * Trim because an uri that comes from the command line may have
     * heading space
     */
    return new DataUriString(uri.trim());
  }

  public static DataUriString create() {
    return new DataUriString();
  }


  public String getPath() {
    if (this.path == null) {
      return null;
    } else {
      if (this.path.charAt(0) == BLOCK_OPEN && this.path.charAt(this.path.length() - 1) == BLOCK_CLOSE) {
        return this.path.substring(1, this.path.length() - 1);
      } else {
        return this.path;
      }
    }
  }


  public String toString() {
    if (this.uri != null) {
      return this.uri;
    } else {
      // We miss the query and fragment parts but they are actually not used
      if (path == null) {
        if (dataStore == null) {
          throw new RuntimeException("A data uri cannot have a path and a datastore null at the same time");
        }
        return AT_CHAR + dataStore;
      } else {
        if (dataStore == null) {
          return path;
        } else {
          return path + AT_CHAR + dataStore;
        }
      }
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
  public int compareTo(DataUriString o) {
    return this.toString().compareTo(o.toString());
  }


  public String getConnectionName() {
    if (dataStore == null) {
      return null;
    } else {
      if (this.dataStore.charAt(0) == BLOCK_OPEN && this.dataStore.charAt(this.path.length() - 1) == BLOCK_CLOSE) {
        return this.dataStore.substring(1, this.dataStore.length() - 1);
      } else {
        return this.dataStore;
      }
    }
  }


  /**
   * A path
   *
   * @param path - the path to the resource
   * @return the object for chaining
   */
  public DataUriString setPath(String path) {
    this.path = path;
    return this;
  }

  public DataUriString setConnection(String name) {
    this.dataStore = name;
    return this;
  }

  /**
   * @param pattern - a glob pattern
   * @return the object for chaining
   */
  public DataUriString setPattern(String pattern) {
    this.path = pattern;
    return this;
  }

  public DataUriString setConnection(Connection connection) {
    this.setConnection(connection.toString());
    return this;
  }

  public String getPattern() {
    return this.path;
  }

  /**
   * The path part may be a data uri
   * in order to define query or command
   * that have no path
   *
   * @return true if the path is a script uri (selector)
   */
  public boolean isScriptSelector() {
    if (path == null) {
      return false;
    }
    return isSubDataUri(path);

  }

  private boolean isSubDataUri(String string) {
    return
      (
        string.charAt(0) == BLOCK_OPEN
          && string.charAt(string.length() - 1) == BLOCK_CLOSE
      );
  }

  public DataUriString getScriptDataUriString() {
    return DataUriString.createFromString(getPath());
  }

  public boolean hasDataUriDataStore() {
    if (dataStore == null) {
      return false;
    }
    return isSubDataUri(dataStore);

  }

  public DataUriString getDataUriDataStore() {
    return DataUriString.createFromString(dataStore);
  }


  public DataUriString setDataUriPath(DataUriString dataUri) {
    this.path = BLOCK_OPEN + dataUri.toString() + BLOCK_CLOSE;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataUriString dataUri = (DataUriString) o;
    return toString().equals(dataUri.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(toString());
  }

  public boolean hasGlobPattern() {
    return Glob.createOf(this.path).containsGlobWildCard();
  }

}
