package net.bytle.db.engine;

import net.bytle.db.DataUri;

public class TableDataUri extends DataUri {

    private final String schemaName;
    private final String tableName;

    public static TableDataUri of(DataUri dataUri) {
        return new TableDataUri(dataUri.toString());
    }


    public String getSchemaName() {
        return schemaName;
    }

    /**
     *
     * @return A table name or a pattern
     */
    public String getTableName() {
        return tableName;
    }

    public TableDataUri(String uri) {

        super(uri);

        String localTableName;
        String localSchemaName;

        localSchemaName = null;
        String[] paths = this.getPathSegments();
        switch (paths.length){
            case 1:
                localTableName = paths[0];
                break;
            case 2:
                localSchemaName = paths[0];
                localTableName = paths[1];
                break;
            default:
                throw new RuntimeException("The database path has ("+ paths.length+") path elements whereas we expect at minimum 1 and maximum 2. The elements are: "+String.join(", ", paths));
        }

        tableName = localTableName;
        schemaName = localSchemaName;
    }

    public static TableDataUri of(String uri) {
        return new TableDataUri(uri);
    }


}
