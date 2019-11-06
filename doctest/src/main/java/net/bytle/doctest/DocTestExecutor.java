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


public class DocTestExecutor {


    public static final String APP_NAME = DocTestExecutor.class.getSimpleName();
    protected final static Log LOGGER_DOCTEST = Log.getLog(DocTestExecutor.class);
    private final String name;
    
    DocCache docCache;
    Map<String, Class> commands = new HashMap<>();

    /**
     * If set to true, the console and the file node will be overwritten
     * @param overwrite
     * @return
     */
    public DocTestExecutor setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    private boolean overwrite = false;


    /**
     * The execution name
     *
     * @param name
     */
    private DocTestExecutor(String name) {
        this.name = name;
    }

    public static List<DocTestRunResult> Run(Path path, String command, Class commandClass) {
        return of("defaultRun").addCommand(command, commandClass).run(path);
    }


    /**
     * @param name - The name of the run (used in the console)
     * @return
     */
    public static DocTestExecutor of(String name) {

        return new DocTestExecutor(name);
    }

    /**
     * 
     * If a docCache is passed, it will be used
     *
     * @param docCache - A doc cache for this run
     * @return a {@link DocTestExecutor} for chaining
     */
    public DocTestExecutor setCache(DocCache docCache) {
        this.docCache = docCache;
        return this;
    }

    /**
     * Execute doc test file and the child of directory defined by the paths parameter
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

            List<Path> childPaths = Fs.getDescendantFiles(path);


            for (Path childPath : childPaths) {

                if (docCache!=null) {
                    String md5Cache = docCache.getMd5(childPath);
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
                if (overwrite) {
                    // Overwrite the new doc
                    Fs.toFile(docTestRunResult.getNewDoc(), childPath);
                }

                if (docCache!=null) {
                    docCache.store(childPath);
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
        DocTestExecutorUnit docTestExecutorUnit = DocTestExecutorUnit.get();
        for (String commandName : commands.keySet()) {
            docTestExecutorUnit.addMainClass(commandName, commands.get(commandName));
        }

        List<DocTestUnit> cachedDocTestUnits = new ArrayList<>();
        if (docCache != null){
            cachedDocTestUnits = docCache.getDocTestUnits(path);
        }
        Integer previousEnd = 0;
        Boolean oneCodeBlockHasAlreadyRun = false;
        for (int i = 0; i < docTests.size(); i++) {

            DocTestUnit docTestUnit = docTests.get(i);
            DocTestUnit cachedDocTestUnit = null;
            if (cachedDocTestUnits != null && i < cachedDocTestUnits.size()) {
                cachedDocTestUnit = cachedDocTestUnits.get(i);
            }
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

                for (int j = 0; j < files.size(); j++) {

                    DocTestFileBlock docTestFileBlock = files.get(j);

                    final String fileStringPath = docTestFileBlock.getPath();
                    if (fileStringPath == null) {
                        throw new RuntimeException("The file path for this unit is null");
                    }
                    // No need of cache test here because it's going very quick
                    if (cachedDocTestUnit!=null) {
                        DocTestFileBlock cachedDocTestFileBlock = cachedDocTestUnit.getFileBlocks().get(j);
                        if (!(fileStringPath.equals(cachedDocTestFileBlock.getPath()))) {
                            fileChange = true;
                        }
                    } else {
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
            if (cachedDocTestUnit!=null) {
                if (!(docTestUnit.getCode().equals(cachedDocTestUnit.getCode()))) {
                    codeChange = true;
                }
            } else {
                codeChange = true;
            }

            // Run
            String result;
            if (
                    ((codeChange || fileChange) & cacheIsOn())
                            || (!cacheIsOn())
                            || oneCodeBlockHasAlreadyRun
            ) {
                try {
                    LOGGER_DOCTEST.info(this.name, "Running the code (" + Log.onOneLine(docTestUnit.getCode()) + ") from the file (" + docTestUnit.getPath() + ")");
                    docTestRunResult.addCodeExecution();
                    result = docTestExecutorUnit.eval(docTestUnit).trim();
                    oneCodeBlockHasAlreadyRun = true;
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


    public DocTestExecutor addCommand(String command, Class mainClazz) {
        commands.put(command, mainClazz);
        return this;
    }

    /**
     * Where do we will find the files defined in the file node
     *
     * @param path
     * @return the runnner for chaining instantiation
     */
    public DocTestExecutor setBaseFileDirectory(Path path) {
        this.baseFileDirectory = path;
        return this;
    }

    private Boolean cacheIsOn(){
        return docCache!=null;
    }

}

