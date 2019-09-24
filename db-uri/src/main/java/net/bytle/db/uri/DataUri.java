package net.bytle.db.uri;

import java.util.Arrays;

public  abstract class DataUri implements IDataUri {

    public static final String PATH_SEPARATOR = "/";
    public static final String AT_STRING = "@";
    private final String databaseName;

    private String[] pathSegments;










    public DataUri(String dataUri, String... parts) {


        if (dataUri==null){
            throw new RuntimeException("The first part of a data uri cannot be null");
        }
        // This is given in a URI form
        if (parts.length==0) {

            final char firstCharacter = dataUri.charAt(0);
            if (firstCharacter != AT_STRING.charAt(0)) {
                throw new RuntimeException("A data uri start with an at sign. Not with (" + firstCharacter + ").");
            }
            String[] pathsParsed = dataUri.substring(1).split(PATH_SEPARATOR);
            this.pathSegments = Arrays.copyOfRange(pathsParsed, 1, pathsParsed.length);
            this.databaseName = pathsParsed[0];

        } else {
            this.pathSegments = parts;
            this.databaseName = dataUri;
        }

    }



    @Override
    public String[] getPathSegments() {
        return this.pathSegments;
    }

    @Override
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
