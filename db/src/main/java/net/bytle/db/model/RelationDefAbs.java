package net.bytle.db.model;

import net.bytle.db.database.Database;

import javax.xml.validation.Schema;
import java.util.Objects;

/**
 * Abstract class that implements the scope data
 */
public abstract class RelationDefAbs implements RelationDef, Comparable<RelationDef> {


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
            // As only the name are knwon
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
        RelationDef tableDef = (RelationDef) o;
        return Objects.equals(getId(), tableDef.getId());

    }

    @Override
    public int hashCode() {

        return Objects.hash(getId());

    }

    /**
     * Ie
     * databaseName+schemaName+tableName
     *
     * @return The unique table ID identifier
     */
    public String getId() {
        return schema.getDatabase().getObjectBuilder().getId(getName(), getSchema().getName());
    }




}
