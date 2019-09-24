package net.bytle.db.uri;

public class TableDataUri extends DataUri {

    private String schemaName;
    private String tableName;

    private TableDataUri(String part, String... parts) {
        super(part, parts);
        init();
    }


    public static TableDataUri of(String part, String... parts) {

        return new TableDataUri(part, parts);

    }



    /**
     *
     * @return A table name or a pattern
     */
    public String getTableName() {
        return tableName;
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
