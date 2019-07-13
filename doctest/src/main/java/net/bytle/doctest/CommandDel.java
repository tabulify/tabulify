package net.bytle.doctest;

import net.bytle.fs.Fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An example of a {@link DocTestUnitExecutor#addMainClass(String, Class)} MainClass}
 * implementing a basic del command
 * <p>
 * This class is used for testing purpose
 * <p>
 * In the documentation, you would see something like that
 * <p>
 * del file.txt
 *
 * Option:
 *   /i - to make it idempotent (ie no error even if the file does not exist)
 */
public class CommandDel {

    public static void main(String[] args) {

        try {
            // Last args
            Path file = Paths.get(args[args.length-1]);
            Boolean idempotent = false;
            if (args.length > 1){
                if (args[0].equals("/i")){
                    idempotent = true;
                }
            }
            if (idempotent){
                if (Files.exists(file)){
                    Files.delete(file);
                }
            } else {
                Files.delete(file);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
