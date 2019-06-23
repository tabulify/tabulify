package net.bytle.doctest;

import net.bytle.cli.CliLog;
import net.bytle.db.engine.Fs;
import net.bytle.db.engine.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class DocTestRunner {

    final static Logger LOGGER = DocTestLogger.LOGGER_DOCTEST;

    private Path baseFileDirectory = Paths.get(".");

    public static DocTestRunner get() {
        return new DocTestRunner();
    }

    /**
     * Run the doc set in the path and
     * @param path
     * @return the new page
     */
    public DocTestRunResult run(Path path) {

        DocTestRunResult docTestRunResult = DocTestRunResult.get(path);

        // Parsing
        List<DocTestUnit> docTests = DocTestParser.getDocTests(path);
        String docTestContent = Fs.getFileContent(path);
        StringBuilder newDocTestContent = new StringBuilder();

        // A runnner
        DocTestCodeRunner docTestCodeRunner = DocTestCodeRunner.get()
                .addMainClass("db", net.bytle.db.cli.Db.class)
                .addMainClass("echo", net.bytle.doctest.DocTestEcho.class); // Just for the test

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
                LOGGER.info("Running the code (" + CliLog.onOneLine(docTestUnit.getCode()) + ")");
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
