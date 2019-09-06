package net.bytle.db.uri;

public class TableDataUri extends DataUri {

    private String schemaName;
    private String tableName;

    public TableDataUri(String[] names) {
        super(names);
    }

    public static TableDataUri ofParts(String... names) {

        return new TableDataUri(names);
    }

    public static TableDataUri ofUri(String uri) {
        return new TableDataUri(uri);
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
        init();

    }

    private void init() {
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
                throw new RuntimeException("In the data uri ("+super.toString()+"), the database path has ("+ paths.length+") path elements whereas we expect at minimum 1 and at maximum 2. The elements are: "+String.join(", ", paths));
        }

        tableName = localTableName;
        schemaName = localSchemaName;
    }

    public String getSchemaName() {
        return schemaName;
    }


}
