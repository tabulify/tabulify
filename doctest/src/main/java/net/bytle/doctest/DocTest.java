package net.bytle.doctest;


import net.bytle.cli.Log;
import net.bytle.fs.Fs;
import net.bytle.type.Strings;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DocTest {


    public static final String APP_NAME = DocTest.class.getName();
    protected final static Log LOGGER_DOCTEST = Log.getLog(DocTest.class);

    private boolean enableCacheExecution = false;
    Map<String, Class> commands = new HashMap<>();
    private boolean overwriteExpectation = false;


    private DocTest() {
    }

    public static List<DocTestRunResult> Run(Path path, String command, Class commandClass) {
        return of().addCommand(command, commandClass).run(path);
    }


    /**
     * Run
     *
     * @param path
     */
    public static List<DocTestRunResult> Run(Path path) {
        return Run(path, null, null);
    }

    public static DocTest of() {

        return new DocTest();
    }

    /**
     * If this is true, a file with the same md5 that has already been executed
     * will not be executed a second time
     * @param b
     * @return
     */
    public DocTest useCacheExecution(boolean b) {
        this.enableCacheExecution = b;
        return this;
    }

    /**
     * @param paths
     * @return
     */
    public List<DocTestRunResult> run(Path... paths) {


        List<DocTestRunResult> results = new ArrayList<>();
        for (Path path : paths) {

            if (!Files.exists(path)) {
                LOGGER_DOCTEST.severe("The path (" + path.toAbsolutePath() + ") does not exist");
                System.exit(1);
            }

            List<Path> childPaths = Fs.getChildFiles(path);


            for (Path childPath : childPaths) {

                if (enableCacheExecution) {
                    String md5Cache = DocCache.get().getMd5(childPath);
                    String md5 = Fs.getMd5(childPath);
                    if (md5.equals(md5Cache)) {
                        LOGGER_DOCTEST.info("Cache is on and the file ("+childPath+") has already been executed. Skipping the execution");
                        DocTestRunResult docTestRunResult = DocTestRunResult.get(childPath);
                        results.add(docTestRunResult);
                        continue;
                    }
                }
                LOGGER_DOCTEST.info("Executing the doc file ("+childPath+")");
                DocTestRunResult docTestRunResult = this.execute(childPath);
                results.add(docTestRunResult);
                if (overwriteExpectation) {
                    // Overwrite the new doc
                    Fs.toFile(docTestRunResult.getNewDoc(), childPath);
                }

                if (enableCacheExecution){
                    DocCache.get().store(childPath);
                }

            }
        }
        return results;

    }

    private Path baseFileDirectory = Paths.get(".");
    // Do we stop at the first execution
    private boolean stopRunAtFirstError = true;

    public void setStopRunAtFirstError(boolean stopRunAtFirstError) {
        this.stopRunAtFirstError = stopRunAtFirstError;
    }


    /**
     * Execute one doc
     *
     * @param path
     * @return the new page
     */
    private DocTestRunResult execute(Path path) {

        DocTestRunResult docTestRunResult = DocTestRunResult
                .get(path)
                .setHasBeenExecuted(true);

        // Parsing
        List<DocTestUnit> docTests = DocTestParser.getDocTests(path);
        String docTestContent = Fs.getFileContent(path);
        StringBuilder newDocTestContent = new StringBuilder();

        // A code executor
        DocTestUnitExecutor docTestUnitExecutor = DocTestUnitExecutor.get();
        for (String commandName : commands.keySet()) {
            docTestUnitExecutor.addMainClass(commandName, commands.get(commandName));
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
                LOGGER_DOCTEST.info("Running the code (" + Log.onOneLine(docTestUnit.getCode()) + ") from the file (" + docTestUnit.getPath() + ")");
                result = docTestUnitExecutor.eval(docTestUnit).trim();
            } catch (Exception e) {
                docTestRunResult.addError();
                if (e.getClass().equals(NullPointerException.class)) {
                    result = "null pointer exception";
                } else {
                    result = e.getMessage();
                }
                LOGGER_DOCTEST.severe("Error during execute: " + result);
                if (stopRunAtFirstError) {
                    throw new RuntimeException(e);
                }
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


    public DocTest addCommand(String command, Class mainClazz) {
        commands.put(command, mainClazz);
        return this;
    }

    /**
     * Where do we will find the files defined in the file node
     *
     * @param path
     * @return the runnner for chaining instantiation
     */
    public DocTest setBaseFileDirectory(Path path) {
        this.baseFileDirectory = path;
        return this;
    }

}

