package net.bytle.db.model;

import net.bytle.db.database.Database;

import javax.xml.validation.Schema;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class that implements the scope data
 */
public abstract class RelationDefAbs implements RelationDef, Comparable<RelationDef> {


    protected RelationMeta meta = new RelationMeta(this);

    public RelationDefAbs(String name) {
        this.name = name;
    }

    /**
     * {@link DatabaseMetaData#getMaxTableNameLength()}
     */
    String name;
    protected SchemaDef schema;
    private String fullyQualifiedName;

    public String getName() {
        return name;
    }

    public Database getDatabase() {
        return schema.getDatabase();
    }

    public SchemaDef getSchema() {
        return schema;
    }

    public String getFullyQualifiedName() {

        if (fullyQualifiedName == null) {
            // The Qualified name is needed for the table build cache
            // As only the name are known
            this.fullyQualifiedName = schema.getDatabase().getObjectBuilder().getFullyQualifiedName(getName(), getSchema().getName());
        }
        return fullyQualifiedName;

    }

    @Override
    public int compareTo(RelationDef o) {
        return this.getFullyQualifiedName().compareTo(o.getFullyQualifiedName());
    }

    @Override
    public String toString() {
        return getFullyQualifiedName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationDefAbs that = (RelationDefAbs) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schema);
    }

    /**
     * Ie
     * databaseName+schemaNameTest+tableName
     *
     * @return The unique table ID identifier
     */
    public String getId() {

        return schema.getDatabase().getObjectBuilder().getId(getName(), getSchema().getName());

    }


    @Override
    public List<ColumnDef> getColumnDefs() {
        return meta.getColumnDefs();
    }

    @Override
    public <T> ColumnDef<T> getColumnDef(String columnName) {
        return meta.getColumnDef(columnName);
    }

    /**
     * @param columnIndex
     * @return a columnDef by index starting at 0
     */
    public ColumnDef getColumnDef(Integer columnIndex) {

        return getColumnDefs().get(columnIndex);
    }


    /**
     * @param columnName - The column name
     * @param clazz - The type of the column (Java needs the type to be a sort of type safe)
     * @return  a new columnDef
     */
    public <T> ColumnDef<T> getColumnOf(String columnName, Class<T> clazz) {

        return meta.getColumnOf(columnName, clazz);

    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you get a exception
     */
    protected ColumnDef[] getColumns(String... columnNames) {

        return meta.getColumns(columnNames);
    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you get a exception
     */
    protected ColumnDef[] getColumns(List<String> columnNames) {

        return meta.getColumns(columnNames.toArray(new String[0]));
    }

}
