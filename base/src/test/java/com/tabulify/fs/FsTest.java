package com.tabulify.fs;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FsTest {

    @Test
    public void baseGetDirectoryNamesInBetweenTest() {
        Path basePath = Paths.get("tmp");
        Path path = Paths.get("tmp", "foo", "bar", "hello");

        List<String> names = Fs.getDirectoryNamesInBetween(path, basePath);
        String[] strings = {"foo", "bar"};
        Assertions.assertEquals(Arrays.asList(strings), names, "There is two names");
    }

    /**
     * basePath does't share the same root with path
     */
    @Test()
    public void failGetDirectoryNamesInBetweenTest() {

        Path basePath = Paths.get("tmp");
        Path path = Paths.get("foo", "bar", "hello");

        Assertions.assertThrows(
                RuntimeException.class,
                () -> Fs.getDirectoryNamesInBetween(path, basePath)
        );

    }

    /**
     * basePath should be path returns nothing
     */
    @Test()
    public void fail2GetDirectoryNamesInBetweenTest() {

        Path basePath = Paths.get("tmp");
        Path path = Paths.get("tmp", "foo", "bar", "hello");

        Assertions.assertThrows(
                RuntimeException.class,
                () -> Fs.getDirectoryNamesInBetween(basePath, path)
        );


    }

    @Test
    public void getPathUntilNameTest() throws FileNotFoundException {

        Path currentDir = Paths.get(".").toAbsolutePath().normalize();
        System.out.println("WorkDir: " + currentDir);
        Path pathToFound = Paths.get("src", "test", "resources", "fs", "foo");
        String nameToFound = "bar";
        Path leafPath = pathToFound.resolve("ni").resolve("co");

        Path foundPath = Fs.closest(leafPath, nameToFound);
        Assertions.assertNotNull(foundPath, "The path should have been found");
        Assertions.assertEquals(pathToFound.toAbsolutePath(), foundPath.getParent(), "The paths should be the same");

        foundPath = Fs.closest(leafPath.toAbsolutePath(), nameToFound);
        Assertions.assertNotNull(foundPath, "The absolute path should have been found ");
        Assertions.assertEquals(pathToFound.toAbsolutePath(), foundPath.getParent(), "The absolute paths should be the same");

    }

    @Test
    public void fsCloserTest() throws FileNotFoundException {

        Path tempDirectory = Fs.getTempDirectory();
        String name = "closer.txt";
        Path closerFileToFind = tempDirectory.resolve(name);
        Fs.write(closerFileToFind, "content");
        Path fileContext = tempDirectory.resolve("directory").resolve("long").resolve("inthedirectory");

        Path closest = Fs.closest(fileContext, name);
        Assertions.assertEquals(closerFileToFind.toAbsolutePath(), closest);

    }

    @Test
    public void setAttribute() throws IOException {
        Path path = Paths.get("src", "test", "resources", "fs", "set-attribute.txt");

        Fs.setUserAttribute(path, "key", "value");

    }

}
