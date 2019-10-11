package net.bytle.db.uri;

import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataUri {

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
}
