# DocTest


## About
A doc runner that takes its unit from the documentation.

See the main class [DocTest](./src/main/java/net/bytle/doctest/DocTestExecutor.java)

In fact, it will for now just replace the content 
  * of the file block with the path defined in the file unit
  * of the console block with the output of the execution of the code block

There is no assertion used.

## Syntax

A unit may have:
  * one or more file block (to replace the content of a file, generally used in the code)
  * zero or one code block (the code to execute)
  * zero or one console block (to get the content of the code execution)

### Pure code

```xhtml
<unit>
    <code java>
        System.out.println("First test");
    </code>
    <console>
        First test
    </console>
</unit>
```

### File replacement

```xhmtl
<unit>
    <file lang path/to/File>
    </file>
</unit>
```

### Command (Class with a main method)

  * Doc Test File

The doc must have an unit with the following format.

```xhtml
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

  * The runner

The base file is where the files reside.

```java
DocTestRunner docTestRunner = DocTestRunner.get()
          .setBaseFileDirectory(Paths.get("./src/test/resources"));

final Path path = Paths.get("./src/test/resources/docTest/fileTest.txt");
DocTestRunResult docTestRun = docTestRunner.run(path,"cat", CommandCat.class);
```
