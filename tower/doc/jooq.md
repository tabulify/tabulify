# Jooq

## About
We tried to used it to generate table class and SQL
but the Vertx Postgres SQL uses `$1` for binding and it's a little bit complicated.


Furthermore, you need to add it in the build process:
* Flyway run
* then read the schema and generate each time
We added on it then on schema change via Java code.

The generation time is not short.

We just create our own table and sql generation.

## Archive


### Plugin configuration

https://www.jooq.org/doc/latest/manual/code-generation/codegen-gradle/
```kts
plugins {
  id("org.jooq.jooq-codegen-gradle") version "3.19.7"
}

dependencies {
  // JOOQ runtime dependencies
  implementation("org.jooq:jooq")
  // Postgres
  // https://www.jooq.org/doc/latest/manual/code-generation/codegen-extensions/codegen-extension-postgres/
  implementation("jooq-postgres-extensions")
  // Your database driver (replace <driver> with the appropriate driver dependency)
  implementation("<driver>")

  // JOOQ code generation dependencies
  // To navigate database schema for code generation or used as a schema crawler as well.
  optional("org.jooq:jooq-meta")
  // To generate your database schema
  optional("org.jooq:jooq-codegen")

}

jooq {
  configuration {
    // shared configuration
    jdbc {
      driver = "oracle.jdbc.OracleDriver"
      url = "jdbc:oracle:thin:@[your jdbc connection parameters]"
      user = "[your database user]"
      password = "[your database password]"

      // You can also pass user/password and other JDBC properties in the optional properties tag:
      properties {
        property {
          key = "prop1"
          value = "valu1"
        }
        property {
          key = "prop2"
          value = "value2"
        }
      }
    }

    database {

      // The database dialect from jooq-meta. Available dialects are
      // named org.jooq.meta.[database].[database]Database.
      name = "org.jooq.meta.postgres.PostgresDatabase"

      // All elements that are generated from your schema (A Java regular expression.
      // Use the pipe to separate several expressions) Watch out for
      // case-sensitivity. Depending on your database, this might be
      // important!
      //
      // You can create case-insensitive regular expressions using this syntax: (?i:expr)
      //
      // Whitespace is ignored and comments are possible.
      includes = ".*"

      // All elements that are excluded from your schema (A Java regular expression.
      // Use the pipe to separate several expressions). Excludes match before
      // includes, i.e. excludes have a higher priority
      excludes = """
           UNUSED_TABLE                # This table (unqualified name) should not be generated
         | PREFIX_.*                   # Objects with a given prefix should not be generated
         | SECRET_SCHEMA\.SECRET_TABLE # This table (qualified name) should not be generated
         | SECRET_ROUTINE              # This routine (unqualified name) ...
      """

      // The schema that is used locally as a source for meta information.
      // This could be your development schema or the production schema, etc
      // This cannot be combined with the schemata element.
      //
      // If left empty, jOOQ will generate all available schemata. See the
      // manual's next section to learn how to generate several schemata
      inputSchema = "[your database schema / owner / name]"

      // postgres
      // https://www.jooq.org/doc/latest/manual/code-generation/codegen-extensions/codegen-extension-postgres/
      forcedTypes {
        forcedType {
          userType = "org.jooq.postgres.extensions.types.Inet"
          binding = "org.jooq.postgres.extensions.bindings.InetBinding"
          includeTypes = "inet"
          priority = "-2147483648"
        }
      }

    }

    // Generation flags: See advanced configuration properties
    generate {
      // https://www.jooq.org/doc/latest/manual/code-generation/codegen-advanced/codegen-config-generate/codegen-generate-java-time-types/
      // Local Date Time
      isJavaTimeTypes = true
    }

    target {

      // The destination package of your generated classes (within the
      // destination directory)
      //
      // jOOQ may append the schema name to this package if generating multiple schemas,
      // e.g. org.jooq.your.packagename.schema1
      // org.jooq.your.packagename.schema2
      packageName = "org.jooq.your.packagename"

      // The destination directory of your generated classes
      directory = "/path/to/your/dir"

    }
  }
  // executions translate to gradle tasks
  executions {
    create("main") {
      configuration {
        // ...
      }
    }

    create("other") {
      configuration {
        // ...
      }
    }
  }
}
```


### Java Generation code

We have generated the Java class with this code
```java
String targetJavaPackageName = jdbcSchema.getTargetJavaPackageName();
if (targetJavaPackageName == null) {
  LOGGER.info("No Schema Class generation for the schema (" + jdbcSchema.getSchemaName() + ")");
  return this;
}
LOGGER.info("Applying JOOQ generation");
JdbcConnectionInfo connectionInfo = this.jdbcClient.getConnectionInfo();

/**
 * Only Postgres for now
 */
String databaseName = connectionInfo.getDatabaseName();
if (!databaseName.equals("postgresql")) {
  throw new DbMigrationException("Jooq for the database (" + databaseName + ") is not configured");
}

/**
 * Database definition
 */
Database database = new Database()
  .withInputSchema(jdbcSchema.getSchemaName())
  .withExcludes(VERSION_LOG_TABLE);

// name
database = database.withName("org.jooq.meta.postgres.PostgresDatabase");
// inet postgres datatype
ForcedType postgresInet = new ForcedType()
  .withUserType("org.jooq.postgres.extensions.types.Inet")
  .withBinding("org.jooq.postgres.extensions.bindings.InetBinding")
  .withIncludeTypes("inet");
  //.withPriority(-2147483648);
database = database.withForcedTypes(postgresInet);


/**
 * JDBC connection
 */
Jdbc jdbc = new Jdbc()
  .withUrl(connectionInfo.getUrl())
  .withUser(connectionInfo.getUser())
  .withPassword(connectionInfo.getPassword());

Configuration configuration = new org.jooq.meta.jaxb.Configuration()
  .withJdbc(jdbc)
  .withGenerator(
    new org.jooq.meta.jaxb.Generator()
      .withDatabase(database)
      .withTarget(
        new Target()
          // current directory is module directory
          .withDirectory("src/main/java")
          .withPackageName(targetJavaPackageName)
      )
  );

try {
  GenerationTool.generate(configuration);
} catch (Exception e) {
  throw new DbMigrationException("Jooq Generation Failed for the schema" + jdbcSchema.getSchemaName(), e);
}
```


### Gradle Dep

```kts
implementation("org.jooq:jooq:$jooqVersion")
// Compile in prod, classpath in dev
// Why? Jooq code generator runs only in dev
if (env === "prod") {
  compileOnly("org.jooq:jooq-meta:$jooqVersion")
  compileOnly("org.jooq:jooq-codegen:$jooqVersion")
} else {
  implementation("org.jooq:jooq-meta:$jooqVersion")
  implementation("org.jooq:jooq-codegen:$jooqVersion")
}
```
