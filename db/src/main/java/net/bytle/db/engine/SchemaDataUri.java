package net.bytle.db.engine;

import net.bytle.db.DataUri;

public class SchemaDataUri extends DataUri {

    private final String schemaName;

    public static SchemaDataUri of(DataUri dataUri) {
        return new SchemaDataUri(dataUri.toString());
    }


    public static SchemaDataUri get(String... parts){
        DataUri dataUri = DataUri.get(parts);
        return of(dataUri);
    }

    public String getSchemaName() {
        return schemaName;
    }


    public SchemaDataUri(String uri) {

        super(uri);

        String localSchemaName;
        String[] paths = this.getPathSegments();
        switch (paths.length){
            case 0:
                // No schema
                localSchemaName = null;
                break;
            case 1:
                localSchemaName = paths[0];
                break;
            default:
                throw new RuntimeException("The schema uri path has ("+ paths.length+") path elements whereas we expect at minimum 0 and maximum 1. The elements are: "+String.join(", ", paths));
        }

        schemaName = localSchemaName;
    }

    public static SchemaDataUri of(String uri) {
        return new SchemaDataUri(uri);
    }


}
