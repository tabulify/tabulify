# Jooq


https://www.jooq.org/doc/latest/manual/code-generation/codegen-version-control/#derived-artefacts

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
