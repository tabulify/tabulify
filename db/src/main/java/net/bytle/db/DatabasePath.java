package net.bytle.db;

public class DatabasePath {

    public static final String PATH_SEPARATOR = "/";
    private final String databaseName;
    private final String schemaName;
    private final String tableName;


    public String getDatabaseName() {
        return databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getFileName() {
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
        if (firstCharacter !="@".charAt(0)){
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
                localSchemaName = databasePathParsed[1];
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
