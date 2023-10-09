package net.bytle.doctest;


import net.bytle.fs.Fs;
import net.bytle.log.Log;
import net.bytle.type.Strings;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


public class DocExecutor {


  public static final String APP_NAME = DocExecutor.class.getSimpleName();
  private final String name;

  private final String eol = Strings.EOL;

  DocCache docCache;
  Map<String, Class<?>> commands = new HashMap<>();

  /**
   *
   *
   * @param overwrite If set to true, the console and the file node will be overwritten
   * @return the object for chaining
   */
  public DocExecutor setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  private boolean overwrite = false;


  /**
   *
   *
   * @param name The execution name
   */
  private DocExecutor(String name) {
    this.name = name;
  }

  public static List<DocExecutorResult> Run(Path path, String command, Class<?> commandClass) {
    return create("defaultRun").addCommand(command, commandClass).run(path);
  }


  /**
   * @param name - The name of the run (used in the console)
   * @return the object for chaining
   */
  public static DocExecutor create(String name) {

    return new DocExecutor(name);
  }

  /**
   * If a docCache is passed, it will be used
   *
   * @param docCache - A doc cache for this run
   * @return a {@link DocExecutor} for chaining
   */
  public DocExecutor setCache(DocCache docCache) {
    this.docCache = docCache;
    return this;
  }

  /**
   * Execute doc test file and the child of directory defined by the paths parameter
   *
   * @param paths the files to execute
   * @return the list of results
   */
  public List<DocExecutorResult> run(Path... paths) {

    DocLog.LOGGER.setLevel(Level.INFO);

    List<DocExecutorResult> results = new ArrayList<>();
    for (Path path : paths) {

      if (!Files.exists(path)) {
        String msg = "The path (" + path.toAbsolutePath() + ") does not exist";
        DocLog.LOGGER.severe(this.name, msg);
        throw new RuntimeException(msg);
      }

      List<Path> childPaths = Fs.getDescendantFiles(path);


      for (Path childPath : childPaths) {

        /**
         * Cache ?
         */
        if (docCache != null) {
          String md5Cache = docCache.getMd5(childPath);
          String md5 = Fs.getMd5(childPath);
          if (md5.equals(md5Cache)) {
            DocLog.LOGGER.info(this.name, "Cache is on and the file (" + childPath + ") has already been executed. Skipping the execution");
            DocExecutorResult docExecutorResult = DocExecutorResult.get(childPath);
            results.add(docExecutorResult);
            continue;
          }
        }

        /**
         * Execution
         */
        DocLog.LOGGER.info(this.name, "Executing the doc file (" + childPath + ")");
        DocExecutorResult docExecutorResult = null;
        try {
          docExecutorResult = this.execute(childPath);
        } catch (NoSuchFileException e) {
          throw new RuntimeException(e);
        }
        results.add(docExecutorResult);
        if (overwrite) {
          // Overwrite the new doc
          Fs.toFile(docExecutorResult.getNewDoc(), childPath);
        }

        if (docCache != null) {
          docCache.store(childPath);
        }

      }
    }
    return results;

  }

  private Path baseFileDirectory = Paths.get(".");

  /**
   * Do we stop at the first error
   */
  private boolean stopRunAtFirstError = true;

  public DocExecutor setStopRunAtFirstError(boolean stopRunAtFirstError) {
    this.stopRunAtFirstError = stopRunAtFirstError;
    return this;
  }


  /**
   *
   *
   * @param path the doc to execute
   * @return the new page
   */
  private DocExecutorResult execute(Path path) throws NoSuchFileException {

    DocExecutorResult docExecutorResult = DocExecutorResult
      .get(path)
      .setHasBeenExecuted(true);

    // Parsing
    List<DocUnit> docTests = DocParser.getDocTests(path);
    String originalDoc = Fs.getFileContent(path);
    StringBuilder targetDoc = new StringBuilder();

    // A code executor
    DocExecutorUnit docExecutorUnit = DocExecutorUnit.create(this);
    for (String commandName : commands.keySet()) {
      docExecutorUnit.addMainClass(commandName, commands.get(commandName));
    }

    List<DocUnit> cachedDocUnits = new ArrayList<>();
    if (docCache != null) {
      cachedDocUnits = docCache.getDocTestUnits(path);
    }
    Integer previousEnd = 0;
    boolean oneCodeBlockHasAlreadyRun = false;
    for (int i = 0; i < docTests.size(); i++) {

      DocUnit docUnit = docTests.get(i);
      DocUnit cachedDocUnit = null;
      if (cachedDocUnits != null && i < cachedDocUnits.size()) {
        cachedDocUnit = cachedDocUnits.get(i);
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
      final List<DocFileBlock> files = docUnit.getFileBlocks();
      if (files.size() != 0) {

        for (int j = 0; j < files.size(); j++) {

          DocFileBlock docFileBlock = files.get(j);

          final String fileStringPath = docFileBlock.getPath();
          if (fileStringPath == null) {
            throw new RuntimeException("The file path for this unit is null (<file type file.extension>");
          }
          // No need of cache test here because it's going very quick
          if (cachedDocUnit != null) {
            List<DocFileBlock> fileBlocks = cachedDocUnit.getFileBlocks();
            if (fileBlocks.size() > j) {
              DocFileBlock cachedDocFileBlock = fileBlocks.get(j);
              if (!(fileStringPath.equals(cachedDocFileBlock.getPath()))) {
                fileChange = true;
              }
            }
          } else {
            fileChange = true;
          }

          Path filePath = Paths.get(baseFileDirectory.toString(), fileStringPath);
          String fileContent = Strings.createFromPath(filePath).toString();

          int start = docFileBlock.getLocationStart();
          targetDoc.append(originalDoc, previousEnd, start);

          DocLog.LOGGER.info(this.name, "Replacing the file block (" + Log.onOneLine(docFileBlock.getPath()) + ") from the file (" + docUnit.getPath() + ")");
          targetDoc
            .append(eol)
            .append(fileContent)
            .append(eol);

          previousEnd = docFileBlock.getLocationEnd();


        }
      }

      // ######################## Code Block Processing #####################
      // Code block is not mandatory, you may just have a file
      String code = docUnit.getCode();
      if (code != null && !code.trim().equals("")) {
        // Check if this unit has already been executed and that the code has not changed
        if (cachedDocUnit != null) {
          if (!(code.equals(cachedDocUnit.getCode()))) {
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
            DocLog.LOGGER.info(this.name, "Running the code (" + Log.onOneLine(code) + ") from the file (" + docUnit.getPath() + ")");
            docExecutorResult.incrementCodeExecutionCounter();
            result = docExecutorUnit.eval(docUnit).trim();
            DocLog.LOGGER.fine(this.name, "Code executed, no error");
            oneCodeBlockHasAlreadyRun = true;
          } catch (Exception e) {
            docExecutorResult.addError();
            if (e.getClass().equals(NullPointerException.class)) {
              result = "null pointer exception";
            } else {
              result = e.getMessage();
            }
            DocLog.LOGGER.severe(this.name, "Error during execute: " + result);
            if (stopRunAtFirstError) {
              DocLog.LOGGER.fine(this.name, "Stop at first run. Throwing the error");
              throw new RuntimeException(e);
            }
          }
        } else {
          DocLog.LOGGER.info(this.name, "The run of the code (" + Log.onOneLine(code) + ") was skipped due to caching from the file (" + docUnit.getPath() + ")");
          result = cachedDocUnit.getConsole();
        }

        // Console
        Integer[] consoleLocation = docUnit.getConsoleLocation();
        if (consoleLocation != null) {
          Integer start = docUnit.getConsoleLocation()[0];
          targetDoc.append(originalDoc, previousEnd, start);
          String console = docUnit.getConsole();
          if (console == null) {
            throw new RuntimeException("No console were found, try a run without cache");
          }
          if (!result.equals(console.trim())) {

            targetDoc
              .append(eol)
              .append(result)
              .append(eol);

            previousEnd = docUnit.getConsoleLocation()[1];

          } else {

            previousEnd = docUnit.getConsoleLocation()[0];

          }
        }
      }
    }
    targetDoc.append(originalDoc, previousEnd, originalDoc.length());
    docExecutorResult.setNewDoc(targetDoc.toString());
    return docExecutorResult;
  }


  public DocExecutor addCommand(String command, Class<?> mainClazz) {
    commands.put(command, mainClazz);
    return this;
  }

  /**
   *
   *
   * @param path the base path (Where do we will find the files defined in the file node)
   * @return the runner for chaining instantiation
   */
  public DocExecutor setBaseFileDirectory(Path path) {
    this.baseFileDirectory = path;
    return this;
  }

  private Boolean cacheIsOn() {
    return docCache != null;
  }

  /**
   * Add java system property
   *
   * @param key the key
   * @param value the value
   * @return the object for chaining
   */
  public DocExecutor setSystemProperty(String key, String value) {
    System.setProperty(key, value);
    return this;
  }

  public boolean doesStopAtFirstError() {
    return this.stopRunAtFirstError;
  }
}
