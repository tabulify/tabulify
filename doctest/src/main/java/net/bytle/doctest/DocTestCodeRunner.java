package net.bytle.doctest;

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
import java.util.logging.Logger;

/**
 * A {@link DocTestCodeRunner} contains the environment variable and function to run a {@link DocTestUnit}
 */
public class DocTestCodeRunner {

    public static final Logger LOGGER = DocTestLogger.LOGGER_DOCTEST;

    /**
     * A map to hold the main class of a appHome. See {@link #addMainClass(String, Class)}
     */
    private HashMap<String, Class> cliClass = new HashMap<String, Class>();

    /**
     * The directory where the compile class are saved
     */
    private Path outputDirClass;

    /**
     * Get a {@link DocTestCodeRunner} with the {@link #get()} function please
     */
    private DocTestCodeRunner() {

        outputDirClass = Paths.get(System.getProperty("java.io.tmpdir"), "docTestClass").normalize().toAbsolutePath();

        try {
            Files.createDirectories(outputDirClass);// Safe if the dir already exist
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return - a docTestRunner that contains the environment variable and function to run a test
     */
    public static DocTestCodeRunner get() {
        return new DocTestCodeRunner();
    }


    /**
     * Run and evaluate the code in a {@link DocTestUnit}
     * This function :
     * * wraps the code in a static method,
     * * run it
     * * capture the stdout and stderr
     * * and return it as a string
     *
     * @param docTestUnit - The docTestUnit to evaluate
     * @return the stdout and stderr in a string
     * @throws Exception - if something is going wrong
     *                   The method {@Link #run} is exception safe and return the error message back
     */
    String eval(DocTestUnit docTestUnit) throws Exception {

        try {

            // The class name
            // The file will have the same name
            // and we will also use it to put it as temporary directory name
            final String className = "javademo";


            // Creation of the java source file
            // You could also extends the SimpleJavaFileObject object as shown in the doc.
            // See SimpleJavaFileObject at https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html
            String code;
            switch (docTestUnit.getLanguage()) {
                case "java":
                    code = "public class " + className + " {" +
                            "public static void run() {\n" +
                            docTestUnit.getCode() +
                            "    }" +
                            "}";
                    break;
                case "dos":
                    String[] args = parseDosCommand(docTestUnit.getCode());
                    final String cli = args[0];
                    Class importClass = this.getMainClass(cli);
                    if (importClass == null) {
                        throw new RuntimeException("No main class was defined for the appHome (" + cli + ")");
                    }
                    args = Arrays.copyOfRange(args, 1, args.length);

                    // Env variable expansion
                    for (Map.Entry<String, String> entry : docTestUnit.getEnv().entrySet()) {
                        for (int i = 0; i < args.length; i++) {
                            args[i] = args[i].replace("%" + entry.getKey() + "%", entry.getValue());
                        }
                    }

                    // Escaping (after env expansion)
                    for (int i = 0; i < args.length; i++) {

                        // Path in DOS must have two slash in the code to escape it
                        args[i] = args[i].replace("\\", "\\\\");

                    }

                    // Code
                    code = "public class " + className + " {\n" +
                            "    public static void run() {\n" +
                            "       " + importClass.getName() + ".main(new String[]{\"" + String.join("\",\"", args) + "\"});\n" +
                            "    }\n" +
                            "}";
                    break;

                default:
                    throw new Exception("Language (" + docTestUnit.getLanguage() + " not yet implemented");
            }
            DocTestSource docTestSource = new DocTestSource(className, code);

            // Verification of the presence of the compilation tool archive
            ClassLoader classLoader = DocTestCodeRunner.class.getClassLoader();
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
            Iterable<? extends JavaFileObject> compilationUnits = Collections.singletonList(docTestSource);
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

            System.setSecurityManager(DocTestSecurityManager.get());

            // Disabling System.exit
            Class javaDemoClass = urlClassLoader.loadClass(className);
            Method method = javaDemoClass.getMethod("run");

            // Capturing outputStream and running the command
            PrintStream backupSystemOut = System.out;
            PrintStream backupSystemErr = System.err;
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(byteArrayOutputStream);
            System.setOut(stream);
            System.setErr(stream);

            // Inovke
            try {
                method.invoke(null);
            } catch (InvocationTargetException e) {
                // An exit was invoked, do nothing
            }

            // Get the output
            System.out.flush(); // Into the byteArray
            System.err.flush(); // Into the byteArray
            System.setOut(backupSystemOut);
            System.setErr(backupSystemErr);

            // Set it back to null
            System.setSecurityManager(null);

            return byteArrayOutputStream.toString();


        } catch (NoSuchMethodException | IOException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {

            throw new RuntimeException(e);

        }


    }

    private String[] parseDosCommand(String code) {

        final int defaultState = 1;
        final int spaceCapture = 2;
        final int quoteCapture = 3;
        final char spaceChar = ' ';
        final char quoteChar = '"';

        int state = defaultState;
        char[] dst = new char[code.length()];
        code.getChars(0, code.length(), dst, 0);
        StringBuilder arg = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (char c : dst) {
            switch (state) {
                case defaultState:
                    switch (c) {
                        case spaceChar:
                            state = spaceCapture;
                            continue;
                        case quoteChar:
                            state = quoteCapture;
                            break;
                        default:
                            arg.append(c);
                            state = spaceCapture;
                            break;
                    }
                    break;
                case spaceCapture:
                    switch (c) {
                        case spaceChar:
                            if (!arg.toString().equals("")) {
                                args.add(arg.toString());
                                arg = new StringBuilder();
                            }
                            state = defaultState;
                            break;
                        case quoteChar:
                            if (!arg.toString().equals("")) {
                                arg.append(c);
                            } else {
                                state = quoteCapture;
                            }
                            break;
                        default:
                            arg.append(c);
                            break;
                    }
                    break;
                case quoteCapture:
                    switch (c) {
                        case quoteChar:
                            if (!arg.toString().equals("")) {
                                args.add(arg.toString());
                                arg = new StringBuilder();
                            }
                            state = defaultState;
                            break;
                        default:
                            arg.append(c);
                            break;
                    }
                    break;
            }
        }

        if (!arg.toString().trim().equals("")) {
            args.add(arg.toString());
        }
        return args.toArray(new String[args.size()]);
    }


    /**
     * Call the function {@link #eval(DocTestUnit)} but is safe of exception
     * It returns the error message if an error occurs
     * The string is also trimmed to suppress the newline and other characters
     *
     * @param docTestUnit - The docTestUnit to run
     * @return the code evaluated or the message error trimmed
     */
    public String run(DocTestUnit docTestUnit) {

        try {
            return eval(docTestUnit).trim();
        } catch (Exception e) {
            return e.getMessage();
        }

    }

    /**
     * If the {@link DocTestUnit#getLanguage() language} is dos or bash,
     * * the first name that we called here appHome is replaced by the mainClass
     * * the others args forms the args that are passed to the main method of the mainClass
     *
     * @param cli       - the appHome (ie the first word in a shell command)
     * @param mainClass - the main class that implements this appHome
     * @return - a docTestRunner for chaining construction
     */
    public DocTestCodeRunner addMainClass(String cli, Class mainClass) {

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
