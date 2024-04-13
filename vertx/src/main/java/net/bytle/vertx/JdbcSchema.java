package net.bytle.vertx;

public class JdbcSchema {
  private final Builder Builder;


  public JdbcSchema(Builder Builder) {
    this.Builder = Builder;
  }

  public static Builder builder(String schemaHandle) {
    return new Builder(schemaHandle);
  }


  public String getLocation() {
    return this.Builder.location;
  }

  public String getSchemaName() {
    return this.Builder.schemaName;
  }

  public String getTargetJavaPackageName() {
    return this.Builder.targetPackage;
  }

  public static class Builder {

    private final String schemaHandle;
    private String location;
    private String targetPackage;
    private String schemaPrefixName = "cs";
    private String schemaName;

    /**
     * @param schemaHandle - the handle without any prefix
     */
    public Builder(String schemaHandle) {
      this.schemaHandle = schemaHandle;

    }

    /**
     *
     * @param location - the location of the migration file
     */
    public Builder setMigrationFileLocation(String location) {
      this.location = location;
      return this;
    }

    /**
     * The prefix is here to be able to make the difference between
     * system schema (such as pg_catalog, public, ...) and combo schema
     */
    public Builder setSchemaPrefix(String prefix) {
      this.schemaPrefixName = prefix;
      return this;
    }

    public JdbcSchema build() {
      if (this.location == null) {
        setMigrationFileLocation("classpath:db/" + schemaPrefixName + "-" + schemaHandle);
      }
      if (schemaName == null) {
        setSchemaName(schemaPrefixName + "_" + schemaHandle);
      }
      return new JdbcSchema(this);
    }

    /**
     * @param schemaName - the database schema name
     */
    public Builder setSchemaName(String schemaName) {
      this.schemaName = schemaName;
      return this;
    }

    /**
     * @param targetPackage - the target package for the generated schema class
     */
    public Builder setJavaPackageForClassGeneration(String targetPackage) {
      this.targetPackage = targetPackage;
      return this;
    }

  }

  @Override
  public String toString() {
    return this.Builder.schemaName;
  }
}
