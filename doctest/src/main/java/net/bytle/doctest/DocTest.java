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


    public static final String APP_NAME = DocTest.class.getSimpleName();
    protected final static Log LOGGER_DOCTEST = Log.getLog(DocTest.class);
    private final String name;

    private boolean enableCacheExecution = false;
    Map<String, Class> commands = new HashMap<>();

    public DocTest setOverwriteConsole(boolean overwriteConsole) {
        this.overwriteConsole = overwriteConsole;
        return this;
    }

    private boolean overwriteConsole = false;


    /**
     * The execution name
     *
     * @param name
     */
    private DocTest(String name) {
        this.name = name;
    }

    public static List<DocTestRunResult> Run(Path path, String command, Class commandClass) {
        return of("defaultRun").addCommand(command, commandClass).run(path);
    }


    /**
     * Run
     *
     * @param path
     */
    public static List<DocTestRunResult> Run(Path path) {
        return Run(path, null, null);
    }

    /**
     * @param name - The name of the run (used in the console)
     * @return
     */
    public static DocTest of(String name) {

        return new DocTest(name);
    }

    /**
     * If this is true, a file with the same md5 that has already been executed
     * will not be executed a second time
     *
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
                LOGGER_DOCTEST.severe(this.name, "The path (" + path.toAbsolutePath() + ") does not exist");
                System.exit(1);
            }

            List<Path> childPaths = Fs.getChildFiles(path);


            for (Path childPath : childPaths) {

                if (enableCacheExecution) {
                    String md5Cache = DocCache.get(name).getMd5(childPath);
                    String md5 = Fs.getMd5(childPath);
                    if (md5.equals(md5Cache)) {
                        LOGGER_DOCTEST.info(this.name, "Cache is on and the file (" + childPath + ") has already been executed. Skipping the execution");
                        DocTestRunResult docTestRunResult = DocTestRunResult.get(childPath);
                        results.add(docTestRunResult);
                        continue;
                    }
                }
                LOGGER_DOCTEST.info(this.name, "Executing the doc file (" + childPath + ")");
                DocTestRunResult docTestRunResult = this.execute(childPath);
                results.add(docTestRunResult);
                if (overwriteConsole) {
                    // Overwrite the new doc
                    Fs.toFile(docTestRunResult.getNewDoc(), childPath);
                }

                if (enableCacheExecution) {
                    DocCache.get(name).store(childPath);
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
        for (int i = 0; i < docTests.size(); i++) {

            DocTestUnit docTestUnit = docTests.get(i);
            DocTestUnit cachedDocTestUnit = DocCache.get(name).getDocTestUnits(path).get(i);
            // Boolean to decide if we need to execute
            boolean codeChange = false;
            boolean fileChange = false;
            // ############################################
            // The order of execution is important here to reconstruct the new document
            //    * First the processing of the file nodes
            //    * then the code
            //    * then the console
            // ############################################

            // Replace file node with the file content on the file system
            final List<DocTestFileBlock> files = docTestUnit.getFileBlocks();
            if (files.size() != 0) {

                for (int j = 0; i < files.size(); i++) {

                    DocTestFileBlock docTestFileBlock = files.get(j);

                    final String fileStringPath = docTestFileBlock.getPath();
                    if (fileStringPath == null) {
                        throw new RuntimeException("The file path for this unit is null");
                    }
                    // No need of cache test here because it's going very quick
                    DocTestFileBlock cachedDocTestFileBlock = cachedDocTestUnit.getFileBlocks().get(j);
                    if (!(fileStringPath.equals(cachedDocTestFileBlock.getPath()))) {
                        fileChange = true;
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

            // ######################## Code Block Processing #####################
            // Check if this unit has already been executed and that the code has not changed
            if (!(docTestUnit.getCode().equals(cachedDocTestUnit.getCode()))) {
                codeChange = true;
            }

            // Run
            String result;
            if (
                    ((codeChange || fileChange) & this.enableCacheExecution)
                    || !this.enableCacheExecution
            ) {
                try {
                    LOGGER_DOCTEST.info(this.name, "Running the code (" + Log.onOneLine(docTestUnit.getCode()) + ") from the file (" + docTestUnit.getPath() + ")");
                    result = docTestUnitExecutor.eval(docTestUnit).trim();
                } catch (Exception e) {
                    docTestRunResult.addError();
                    if (e.getClass().equals(NullPointerException.class)) {
                        result = "null pointer exception";
                    } else {
                        result = e.getMessage();
                    }
                    LOGGER_DOCTEST.severe(this.name, "Error during execute: " + result);
                    if (stopRunAtFirstError) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                LOGGER_DOCTEST.info(this.name, "The run of the code (" + Log.onOneLine(docTestUnit.getCode()) + ") was skipped due to caching from the file (" + docTestUnit.getPath() + ")");
                result = cachedDocTestUnit.getConsole();
            }

            // Console
            Integer[] consoleLocation = docTestUnit.getConsoleLocation();
            if (consoleLocation != null) {
                Integer start = docTestUnit.getConsoleLocation()[0];
                newDocTestContent.append(docTestContent, previousEnd, start);
                if (!result.equals(docTestUnit.getConsole())) {

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

