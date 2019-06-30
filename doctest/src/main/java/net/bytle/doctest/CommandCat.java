package net.bytle.doctest;

import net.bytle.fs.Fs;

import java.nio.file.Paths;

/**
 * An example of a {@link DocTestUnitExecutor#addMainClass(String, Class)} MainClass}
 * implementing a basic cat command
 * <p>
 * This class is used for testing purpose
 * <p>
 * In the documentation, you would see something like that
 * <p>
 * cat file.txt
 */
public class CommandCat {

    public static void main(String[] args) {

        System.out.println(Fs.getFileContent(Paths.get(args[0])));
    }

}
