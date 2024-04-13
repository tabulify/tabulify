package net.bytle.vertx;

public class JdbcSchema {
  private final Builder Builder;


  public JdbcSchema(Builder Builder) {
    this.Builder = Builder;
  }

  public static Builder builder(String schemaHandle) {
    return new Builder(schemaHandle);
  }

  public static JdbcSchema createFromHandle(String schemaHandle) {
    return builder(schemaHandle).build();
  }

  public String getLocation() {
    return this.Builder.location;
  }

  public String getSchema() {
    return this.Builder.schema;
  }

  public String getTargetJavaPackageName() {
    return this.Builder.targetPackage;
  }

  public static class Builder {
    public String schema;
    private String location;
    private String targetPackage;

    /**
     * @param schemaHandle - the handle without any prefix
     */
    public Builder(String schemaHandle) {
      setMigrationFileLocation("classpath:db/cs-" + schemaHandle);
      setSchema(JdbcSchemaManager.getSchemaFromHandle(schemaHandle));
    }

    /**
     *
     * @param location - the location of the migration file
     */
    public Builder setMigrationFileLocation(String location) {
      this.location = location;
      return this;
    }

    public JdbcSchema build() {
      return new JdbcSchema(this);
    }

    /**
     * @param schema - the database schema name
     */
    public Builder setSchema(String schema) {
      this.schema = schema;
      return this;
    }

    /**
     * @param targetPackage - the target package for the generated schema class
     */
    public Builder setTargetPackage(String targetPackage) {
      this.targetPackage = targetPackage;
      return this;
    }

  }

  @Override
  public String toString() {
    return this.Builder.schema;
  }
}
