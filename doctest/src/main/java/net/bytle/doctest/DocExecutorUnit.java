package net.bytle.doctest;

import net.bytle.log.Log;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

/**
 * Execute a code block found in a doc
 * <p>
 * A {@link DocExecutorUnit} contains the environment variable and function to run a {@link DocUnit}
 */
public class DocExecutorUnit {

  public static final Log LOGGER = DocLog.LOGGER;
  private final DocExecutor docExecutor;

  /**
   * A map to hold the main class of a appHome. See {@link #addMainClass(String, Class)}
   */
  private HashMap<String, Class> cliClass = new HashMap<String, Class>();

  /**
   * The directory where the compile class are saved
   */
  private Path outputDirClass;

  /**
   * Get a {@link DocExecutorUnit} with the {@link #create(DocExecutor)} function please
   * @param docExecutor
   */
  private DocExecutorUnit(DocExecutor docExecutor) {

    outputDirClass = Paths.get(System.getProperty("java.io.tmpdir"), "docTestClass").normalize().toAbsolutePath();
    this.docExecutor = docExecutor;
    try {
      Files.createDirectories(outputDirClass);// Safe if the dir already exist
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return - a docTestRunner that contains the environment variable and function to run a test
   * @param docExecutor
   */
  public static DocExecutorUnit create(DocExecutor docExecutor) {
    return new DocExecutorUnit(docExecutor);
  }


  /**
   * Run and evaluate the code in a {@link DocUnit}
   * This function :
   * * wraps the code in a static method,
   * * run it
   * * capture the stdout and stderr
   * * and return it as a string
   *
   * @param docUnit - The docTestUnit to evaluate
   * @return the stdout and stderr in a string
   * @throws RuntimeException - if something is going wrong
   *                          The method {@Link #run} is exception safe and return the error message back
   */
  String eval(DocUnit docUnit) {

    try {

      // The class name that will be created
      // The file will have the same name
      // and we will also use it to put it as temporary directory name
      final String buildClassName = "javademo";
      final String runMethodName = "run";


      // Creation of the java source file
      // You could also extends the SimpleJavaFileObject object as shown in the doc.
      // See SimpleJavaFileObject at https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html
      String code;
      switch (docUnit.getLanguage()) {
        case "java":

          code = "public class " + buildClassName + " {" +
            "public static void " + runMethodName + "() {\n" +
            docUnit.getCode() +
            "    }" +
            "}";
          break;
        case "dos":
        case "bash":
          List<String[]> commands = DocDos.parseDosCommand(docUnit);
          StringBuilder javaCode = new StringBuilder();
          for (String[] command : commands) {
            String[] args = command;
            final String cli = args[0];
            Class importClass = this.getMainClass(cli);
            if (importClass == null) {
              throw new RuntimeException("No main class was defined for the command (" + cli + ")");
            }
            args = Arrays.copyOfRange(args, 1, args.length);
            javaCode
              .append(importClass.getName())
              .append(".main(new String[]{\"")
              .append(String.join("\",\"", args))
              .append("\"});\n");
          }
          // Code
          code = "public class " + buildClassName + " {\n" +
            "    public static void " + runMethodName + "() {\n" +
            "       " + javaCode.toString() +
            "    }\n" +
            "}";
          break;

        default:
          throw new RuntimeException("Language (" + docUnit.getLanguage() + " not yet implemented");
      }
      DocSource docSource = new DocSource(buildClassName, code);

      // Verification of the presence of the compilation tool archive
      ClassLoader classLoader = DocExecutorUnit.class.getClassLoader();
      final String toolsJarFileName = "tools.jar";
      String javaHome = System.getProperty("java.home");
      Path toolsJarFilePath = Paths.get(javaHome, "lib", toolsJarFileName);
      if (!Files.exists(toolsJarFilePath)) {
        LOGGER.fine("The tools jar file (" + toolsJarFileName + ") could not be found at (" + toolsJarFilePath + ") but it may still work.");
      }

      // The compile part
      // Get the compiler
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if (compiler == null) {

        final String message = "Unable to get the system Java Compiler. Are your running java with a JDK ?";
        LOGGER.severe(message);
        LOGGER.severe("Java Home: " + javaHome);
        throw new RuntimeException(message);

      }

      // Create a compilation unit (files)
      Iterable<? extends JavaFileObject> compilationUnits = Collections.singletonList(docSource);
      // A feedback object (diagnostic) to get errors
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

      // Javac options here
      List<String> options = new ArrayList<>();
      options.add("-d");
      options.add(outputDirClass.toString());

      // Compilation unit can be created and called only once
      JavaCompiler.CompilationTask task = compiler.getTask(
        null,
        null,
        diagnostics,
        options,
        null,
        compilationUnits
      );
      // The compile task is called
      task.call();
      // Printing of any compile problems
      for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
        final String msg = "Error on line " + diagnostic.getLineNumber() + " source " + diagnostic.getSource();
        LOGGER.fine(msg);
        throw new RuntimeException(msg + "\n" + code);
      }


      // Now that the class was created, we will load it and run it
      LOGGER.fine("Trying to load from " + outputDirClass);
      URLClassLoader urlClassLoader = new URLClassLoader(
        new URL[]{outputDirClass.toUri().toURL()},
        classLoader);

      // Disabling System.exit with the security manager
      if (!this.docExecutor.doesStopAtFirstError()) {
        System.setSecurityManager(DocSecurityManager.create());
      }

      // Loading the dynamically build class
      Class buildClass = urlClassLoader.loadClass(buildClassName);
      Method method = buildClass.getMethod(runMethodName);

      // Capturing outputStream and running the command
      PrintStream backupSystemOut = System.out;
      PrintStream backupSystemErr = System.err;
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      PrintStream stream = new PrintStream(byteArrayOutputStream);
      System.setOut(stream);
      System.setErr(stream);
      // Invoke
      try {
        method.invoke(null);
      } catch (InvocationTargetException e) {
        // Is this a System.exit(0) as we have after the print of a help command
        if (!e.getTargetException().getClass().equals(PreventExitException.class)) {
          // Error
          System.out.flush(); // Into the byteArray
          System.err.flush(); // Into the byteArray
          throw new RuntimeException("Error has been seen. Console Output was: \n" + byteArrayOutputStream.toString(), e);
        } else {
          DocLog.LOGGER.info("Error was seen prevented");
        }
      } finally {

        // Reset the log level
        DocLog.LOGGER.setLevel(Level.INFO);
        // Get the output
        System.out.flush(); // Into the byteArray
        System.err.flush(); // Into the byteArray
        System.setOut(backupSystemOut);
        System.setErr(backupSystemErr);

        // Set it back to null
        System.setSecurityManager(null);

      }

      return byteArrayOutputStream.toString();

    } catch (NoSuchMethodException | IOException | IllegalAccessException | ClassNotFoundException e) {

      throw new RuntimeException(e);

    }


  }


  /**
   * Call the function {@link #eval(DocUnit)} but is safe of exception
   * It returns the error message if an error occurs
   * The string is also trimmed to suppress the newline and other characters
   *
   * @param docUnit - The docTestUnit to run
   * @return the code evaluated or the message error trimmed
   */
  public String run(DocUnit docUnit) {


    return eval(docUnit).trim();


  }

  /**
   * If the {@link DocUnit#getLanguage() language} is dos or bash,
   * * the first name that we called here appHome is replaced by the mainClass
   * * the others args forms the args that are passed to the main method of the mainClass
   *
   * @param cli       - the appHome (ie the first word in a shell command)
   * @param mainClass - the main class that implements this appHome
   * @return - a docTestRunner for chaining construction
   */
  public DocExecutorUnit addMainClass(String cli, Class mainClass) {

    this.cliClass.put(cli, mainClass);
    return this;
  }

  /**
   * @param cli
   * @return the main class that implements a appHome
   * <p>
   * This is used to generate Java code when the documentation is a shell documentation
   */
  public Class getMainClass(String cli) {
    return this.cliClass.get(cli);
  }

}
