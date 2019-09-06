package net.bytle.db.uri;

public class SchemaDataUri extends DataUri {

    private String schemaName;

    public SchemaDataUri(String[] parts) {
        super(parts);
        init();
    }

    public static SchemaDataUri ofUri(String uri) {

        return new SchemaDataUri(uri);
    }


    public static SchemaDataUri ofParts(String... parts) {

        return new SchemaDataUri(parts);

    }

    public String getSchemaName() {
        return schemaName;
    }


    public SchemaDataUri(String uri) {

        super(uri);
        init();

    }

    private void init() {
        String localSchemaName;
        String[] paths = this.getPathSegments();
        switch (paths.length) {
            case 0:
                // No schema
                localSchemaName = null;
                break;
            case 1:
                localSchemaName = paths[0];
                break;
            default:
                throw new RuntimeException("The schema uri path has (" + paths.length + ") path elements whereas we expect at minimum 0 and maximum 1. The elements are: " + String.join(", ", paths));
        }

        schemaName = localSchemaName;
    }


}
