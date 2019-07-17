package net.bytle.db;

import java.util.Arrays;

public class DataUri {

    public static final String PATH_SEPARATOR = "/";
    public static final String AT_STRING = "@";
    private final String databaseName;

    String[] pathSegments;

    /**
     *
     * @param parts
     * @return a path separator from an array of parts (ie @part1/part2/part3)
     */
    public static DataUri get(String... parts) {
        StringBuilder stringBuilder = new StringBuilder();
        if (parts[0].charAt(0) != AT_STRING.charAt(0)){
            stringBuilder.append(AT_STRING);

        }
        stringBuilder.append(String.join(PATH_SEPARATOR,parts));
        return new DataUri(stringBuilder.toString());

    }


    public DataUri(String dataUri) {


        if (dataUri==null){
            throw new RuntimeException("A data uri cannot be null");
        }
        if (dataUri.length()<=0){
            throw new RuntimeException("A data uri cannot be an empty string");
        }
        final char firstCharacter = dataUri.charAt(0);
        if (firstCharacter != AT_STRING.charAt(0)){
            throw new RuntimeException("A data uri start with an at sign. Not with ("+ firstCharacter +").");
        }
        String[] pathsParsed = dataUri.substring(1).split(PATH_SEPARATOR);
        this.pathSegments = Arrays.copyOfRange(pathsParsed,1,pathsParsed.length);
        this.databaseName = pathsParsed[0];

    }

    public static DataUri of(String databasePath) {
        return new DataUri(databasePath);
    }

    public String[] getPathSegments() {
        return this.pathSegments;
    }

    public String getDatabaseName(){
        return this.databaseName;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("@").append(databaseName) ;
        if (getPathSegments().length>0){
            stringBuilder
                    .append("/")
                    .append(String.join("/",pathSegments));
        }
        return stringBuilder.toString();
    }
}
