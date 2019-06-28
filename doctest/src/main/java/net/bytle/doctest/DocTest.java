package net.bytle.doctest;

import net.bytle.fs.Fs;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class DocTest {


    final static Logger LOGGER = DocTestLogger.LOGGER_DOCTEST;

    public static void Run(Path path, String command, Class commandClass) {
        Run(path,command,commandClass, Paths.get("."));
    }

    public static void Run(Path path, String command, Class commandClass, Path baseFileDirectory) {


        if (!Files.exists(path)) {
            LOGGER.severe("The path (" + path + ") does not exist");
            System.exit(1);
        }
        List<Path> paths = Fs.getChildFiles(path);

        int errorCount = 0;
        DocTestRunner docTestRunner = DocTestRunner.get()
                .setBaseFileDirectory(baseFileDirectory);
        Map<String, Class> commands = new HashMap<>();
        commands.put(command, commandClass);
        for (Path childPath : paths) {

            DocTestRunResult docTestRunResult = docTestRunner.run(childPath, commands);
            errorCount += docTestRunResult.getErrors();
            Fs.toFile(docTestRunResult.getNewDoc(), childPath);

        }

        if (errorCount != 0) {
            final String msg = errorCount + " errors were seen during documentation execution.";
            System.err.println(msg);
            LOGGER.severe(msg);
        }

    }


}

