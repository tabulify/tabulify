# DocTest


## About
A test runner that takes its unit test from the documentation.


See the main class [DocTest](./src/main/java/net/bytle/doctest/DocTest.java)

## Syntax

### Pure code

```java
<unit>
    <code java>
        System.out.println("First test");
    </code>
    <console>
        First test
    </console>
</unit>
```

### Command (Class with a main method)

  * Doc Test File

The doc must have an unit with the following format.

```xml
<unit envHOME=Whatever>
    <file lang path/to/File>
    </file>
    <code dos>
        echo %HOME%
    </code>
    <console>
        Whatever
    </console>
</unit>
```

  * The runnner

The base file is where the files reside.

```java
DocTestRunner docTestRunner = DocTestRunner.get()
          .setBaseFileDirectory(Paths.get("./src/test/resources"));

final Path path = Paths.get("./src/test/resources/docTest/fileTest.txt");
DocTestRunResult docTestRun = docTestRunner.run(path,"cat", CommandCat.class);
```
