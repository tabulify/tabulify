package net.bytle.doctest;

import net.bytle.fs.Fs;
import net.bytle.log.Log;
import net.bytle.type.Strings;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DocTestRunner {

    final static Logger LOGGER = DocTestLogger.LOGGER_DOCTEST;

    /**
     * Settings the root path for the files
     * @param baseFileDirectory
     */
    public DocTestRunner setBaseFileDirectory(Path baseFileDirectory) {
        this.baseFileDirectory = baseFileDirectory;
        return this;
    }

    private Path baseFileDirectory = Paths.get(".");

    public static DocTestRunner get() {
        return new DocTestRunner();
    }

    /**
     * TODO: Create a dry run to check for the block positions (ie file > code > console)
     * Run the doc set in the path and
     * @param path
     * @return the new page
     */
    public DocTestRunResult run(Path path, Map<String,Class> commands) {

        DocTestRunResult docTestRunResult = DocTestRunResult.get(path);

        // Parsing
        List<DocTestUnit> docTests = DocTestParser.getDocTests(path);
        String docTestContent = Fs.getFileContent(path);
        StringBuilder newDocTestContent = new StringBuilder();

        // A runnner
        DocTestCodeRunner docTestCodeRunner = DocTestCodeRunner.get();
        for (String commandName :commands.keySet()){
            docTestCodeRunner.addMainClass(commandName,commands.get(commandName));
        }


        Integer previousEnd = 0;
        for (DocTestUnit docTestUnit : docTests) {

            // Replace file node with the file content on the file system
            final List<DocTestFileBlock> files = docTestUnit.getFileBlocks();
            if (files.size() != 0) {

                for (DocTestFileBlock docTestFileBlock : files) {

                    final String fileStringPath = docTestFileBlock.getPath();
                    if (fileStringPath == null) {
                        throw new RuntimeException("The file path for this unit is null");
                    }
                    Path filePath = Paths.get(baseFileDirectory.toString(), fileStringPath);
                    String fileContent = Strings.get(filePath);

                    Integer start = docTestFileBlock.getLocationStart();
                    newDocTestContent.append(docTestContent, previousEnd, start);


                    newDocTestContent
                            .append("\r\n")
                            .append(fileContent)
                            .append("\r\n");

                    previousEnd = docTestFileBlock.getLocationEnd();


                }
            }
            String result;
            try {
                LOGGER.info("Running the code (" + Log.onOneLine(docTestUnit.getCode()) + ") from the file ("+docTestUnit.getPath()+")" );
                result = docTestCodeRunner.eval(docTestUnit).trim();
            } catch (Exception e) {
                docTestRunResult.addError();
                if (e.getClass().equals(NullPointerException.class)) {
                    result = "null pointer exception";
                } else {
                    result = e.getMessage();
                }
                LOGGER.severe("Error during docTestRun: " + result);
                e.printStackTrace(System.err);
            }

            // Console
            Integer[] consoleLocation = docTestUnit.getConsoleLocation();
            if (consoleLocation != null) {
                Integer start = docTestUnit.getConsoleLocation()[0];
                newDocTestContent.append(docTestContent, previousEnd, start);
                if (!result.equals(docTestUnit.getExpectation())) {

                    newDocTestContent
                            .append("\r\n")
                            .append(result)
                            .append("\r\n");

                    previousEnd = docTestUnit.getConsoleLocation()[1];

                } else {

                    previousEnd = docTestUnit.getConsoleLocation()[0];

                }
            }
        }
        newDocTestContent.append(docTestContent, previousEnd, docTestContent.length());
        docTestRunResult.setNewDoc(newDocTestContent.toString());
        return docTestRunResult;
    }

    public DocTestRunResult run(Path path, String command, Class mainClazz) {
        Map<String,Class> commands = new HashMap<>();
        commands.put(command,mainClazz);
        return run(path,commands);
    }

    /**
     * Where do we will find the files defined in the file node
     *
     * @param path
     * @return the runnner for chaining instantiation
     */
//    public DocTestRunner setBaseFileDirectory(Path path) {
//        this.baseFileDirectory = path;
//        return this;
//    }
}
