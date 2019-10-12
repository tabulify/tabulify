package net.bytle.db.uri;

import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataUri implements Comparable<DataUri> {

    public static final String PATH_SEPARATOR = "/";
    public static final String AT_STRING = "@";
    private final Database dataStore;
    private List<String> pathSegments;



    private DataUri(Database dataStore, String... more) {

        this.dataStore = dataStore;
        this.pathSegments = new ArrayList<>();
        this.pathSegments.addAll(Arrays.asList(more));

    }

    private DataUri(DatabasesStore dataStorePath, String first, String... more) {


        if (first == null) {
            throw new RuntimeException("The first part of a data uri cannot be null");
        }

        if (more.length == 0) {

            final char firstCharacter = first.charAt(0);
            if (firstCharacter != AT_STRING.charAt(0)) {

                this.pathSegments = new ArrayList<>();
                this.pathSegments.addAll(Arrays.asList(first.split(PATH_SEPARATOR)));
                this.pathSegments.addAll(Arrays.asList(more));
                this.dataStore = dataStorePath.getDefaultDatabase();


            } else {

                String[] pathsParsed = first.substring(1).split(PATH_SEPARATOR);
                this.pathSegments = Arrays.asList(Arrays.copyOfRange(pathsParsed, 1, pathsParsed.length));
                this.dataStore = dataStorePath.getDatabase(pathsParsed[0]);

            }

        } else {

            final char firstCharacter = first.charAt(0);
            if (firstCharacter != AT_STRING.charAt(0)) {

                this.pathSegments = new ArrayList<>();
                this.pathSegments.add(first);
                this.pathSegments.addAll(Arrays.asList(more));
                this.dataStore = dataStorePath.getDefaultDatabase();

            } else {

                this.pathSegments = Arrays.asList(more);
                this.dataStore = dataStorePath.getDatabase(first.substring(1));

            }

        }

    }


    /**
     *
     * @param first
     * @param more
     * @return a data uri from the default datastore
     */
    public static DataUri of(String first, String... more) {
        return new DataUri(DatabasesStore.of(),first, more);
    }


    public static DataUri of(Path dataStorePath, String dataStoreName, String... more) {
        return new DataUri(DatabasesStore.of(dataStorePath).getDatabase(dataStoreName), more);
    }


    public List<String> getPathSegments() {
        return this.pathSegments;
    }

    public Database getDataStore() {
        return this.dataStore;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("@").append(dataStore);
        if (getPathSegments().size() > 0) {
            stringBuilder
                    .append("/")
                    .append(String.join("/", pathSegments));
        }
        return stringBuilder.toString();
    }


    public String getDataName() {
        return this.pathSegments.get(this.pathSegments.size()-1);
    }


    public String getPathSegment(int i) {
        return this.pathSegments.get(i);
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
}
