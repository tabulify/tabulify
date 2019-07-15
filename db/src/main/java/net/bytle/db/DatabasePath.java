package net.bytle.db;

public class DatabasePath {

    public static final String PATH_SEPARATOR = "/";
    public static final String AT_STRING = "@";
    private final String databaseName;
    private final String schemaName;
    private final String tableName;

    /**
     *
     * @param parts
     * @return a path separator from an array of parts (ie @part1/part2/part3)
     */
    public static String get(String... parts) {
        return AT_STRING+String.join(PATH_SEPARATOR,parts);
    }


    public String getDatabaseName() {
        return databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    /**
     * It can be a glob or a name
     * @return
     */
    public String getDestinationPart() {
        return tableName;
    }

    public DatabasePath(String databasePath) {
        String localTableName;
        String localSchemaName;
        String localDatabaseName;
        if (databasePath==null){
            throw new RuntimeException("A database table path cannot be null");
        }
        if (databasePath.length()<=0){
            throw new RuntimeException("A database table path cannot be an empty string");
        }
        final char firstCharacter = databasePath.charAt(0);
        if (firstCharacter != AT_STRING.charAt(0)){
            throw new RuntimeException("A database table path start with an at sign. Not with ("+ firstCharacter +").");
        }
        String[] databasePathParsed = databasePath.substring(1).split(PATH_SEPARATOR);

        localSchemaName = null;
        localTableName = null;
        switch (databasePathParsed.length){
            case 1:
                localDatabaseName = databasePathParsed[0];
                break;
            case 2:
                localDatabaseName = databasePathParsed[0];
                localTableName = databasePathParsed[1];
                break;
            case 3:
                localDatabaseName = databasePathParsed[0];
                localSchemaName = databasePathParsed[1];
                localTableName = databasePathParsed[2];
                break;
                default:
                    throw new RuntimeException("The database path has ("+databasePathParsed.length+") path elements whereas we expect maximum 3. The elements are: "+String.join(", ",databasePathParsed));
        }

        tableName = localTableName;
        schemaName = localSchemaName;
        databaseName = localDatabaseName;
    }

    public static DatabasePath of(String databasePath) {
        return new DatabasePath(databasePath);
    }
}
