package com.tabulify.exec;


import com.tabulify.fs.Fs;
import com.tabulify.text.plain.TextCharacterSetNotDetected;
import com.tabulify.text.plain.TextDetectedCharsetNotSupported;
import com.tabulify.text.plain.TextFile;
import com.tabulify.java.JavaEnvs;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Execute a executable or a script based on its shebang
 */
public class OsExec {

    private final OsExecBuilder builder;

    public OsExec(OsExecBuilder osExecBuilder) {
        this.builder = osExecBuilder;
    }

    /**
     * Executes a script based on its shebang line
     *
     * @return ProcessResult containing exit code, output, and error streams
     */
    public ProcessResult execute() {

        List<String> command = new ArrayList<>(this.builder.shebangArguments);


        // Add script path
        // May be absolute or relative
        command.add(this.builder.executablePath.toString());

        // Add additional arguments
        if (this.builder.args != null) {
            command.addAll(this.builder.args);
        }

        ProcessExecutor processExecutor = new ProcessExecutor()
                .directory(this.builder.workingDirectory.toFile())
                .command(command);

        if (this.builder.timeout != null) {
            processExecutor
                    .timeout(this.builder.timeout, this.builder.timeoutUnit);
        }

        /**
         * Mandatory to get the stream
         * Otherwise we get
         * java.lang.IllegalStateException: Process output was not read. To enable output reading please call ProcessExecutor.readOutput(true) before starting the process.
         */
        processExecutor.readOutput(true);


        /**
         * Redirect Output
         */
        OutputStream out = null;
        if (this.builder.ouputPath != null) {

            // Create the parent directory if it does not exist
            Fs.createDirectoryIfNotExists(this.builder.ouputPath.getParent());

            // Open or create the file with the option (ie CREATE, and WRITE)
            // TRUNCATE_EXISTING is in the default, we don't want that
            try {
                out = Files.newOutputStream(this.builder.ouputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException("We could not create or open the output path (" + this.builder.ouputPath + "). Error: " + e.getMessage(), e);
            }

            processExecutor.redirectOutput(out);

        }
        OutputStream err = null;
        if (this.builder.errorPath != null) {

            // Create the parent directory if it does not exist
            Fs.createDirectoryIfNotExists(this.builder.errorPath.getParent());

            // Open or create the file with the option (ie CREATE, and WRITE)
            // TRUNCATE_EXISTING is in the default, we don't want that
            try {
                err = Files.newOutputStream(this.builder.errorPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException("We could not create or open the error path (" + this.builder.errorPath + "). Error: " + e.getMessage(), e);
            }

            processExecutor.redirectError(err);

        } else {

            /**
             * By default, zt-exec redirect to standard output, we don't want that
             * Not sure where {@link System.err} goes when this is not a {@link JavaEnvs#isRunningInTerminal()}
             * If it does not work, OutputStream.nullOutputStream()
             */
            OutputStream output = new PrintStream(System.err);
            processExecutor.redirectError(output);
        }

        // Execute the script
        try {

            return processExecutor.execute();

        } catch (IOException | InterruptedException | TimeoutException e) {

            throw new RuntimeException("Error while executing the path (" + this.builder.ouputPath + "). Error: " + e.getMessage(), e);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Some failures of the close method can be safely ignored (e.g., closing a file that was open for read).
                    // https://stackoverflow.com/questions/6889697/close-resource-quietly-using-try-with-resources
                    System.err.println("Failed to close the output stream.");
                }
            }
            if (err != null) {
                try {
                    err.close();
                } catch (IOException e) {
                    // Some failures of the close method can be safely ignored (e.g., closing a file that was open for read).
                    // https://stackoverflow.com/questions/6889697/close-resource-quietly-using-try-with-resources
                    System.err.println("Failed to close the error stream.");
                }
            }
        }


    }


    public static OsExecBuilder builder() {
        return new OsExecBuilder();
    }

    @SuppressWarnings("unused")
    public static class OsExecBuilder {


        private Charset charset;
        private Long timeout;
        private TimeUnit timeoutUnit;

        public Path ouputPath;
        public int streamBufferSize = 8192;

        private Path workingDirectory;
        private Path errorPath;


        public OsExecBuilder setStandardOutputPath(Path ouputPath) {
            this.ouputPath = ouputPath;
            return this;
        }

        public OsExecBuilder setStandardErrorPath(Path errorPath) {
            this.errorPath = errorPath;
            return this;
        }

        public OsExecBuilder setStandardStreamBufferSize(int outputStreamBufferSize) {
            this.streamBufferSize = outputStreamBufferSize;
            return this;
        }

        /**
         * Relative to the working directory or absolute
         * The user choose
         */
        private Path executablePath;
        private List<String> args = new ArrayList<>();
        private boolean executableOnly = true;
        /**
         * If this is a text file, we detect the shebang arguments
         */
        private List<String> shebangArguments = new ArrayList<>();

        public OsExecBuilder setExecutableOnly(boolean executableOnly) {
            this.executableOnly = executableOnly;
            return this;
        }

        public void setTimeout(long timeout, TimeUnit timeoutUnit) {
            assert timeout > 0;
            assert timeoutUnit != null;
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
        }

        /**
         * @param executablePath Path to the executable / script file
         */
        public OsExecBuilder setExecutablePath(Path executablePath) {
            this.executablePath = executablePath.toAbsolutePath();
            return this;
        }

        public void setCharset(Charset charset) {
            this.charset = charset;
        }

        public OsExecBuilder setWorkingDirectory(Path workingDirectory) {

            if (!Files.exists(workingDirectory)) {
                try {
                    Files.createDirectories(workingDirectory);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            this.workingDirectory = workingDirectory;
            return this;

        }

        /**
         * @param args Additional arguments to pass to the executable
         */
        public OsExecBuilder setArguments(String... args) {
            this.args = List.of(args);
            return this;
        }

        public OsExecBuilder setArguments(List<String> args) {
            this.args = args;
            return this;
        }

        public OsExec build() {
            // Check if file exists and is readable
            if (!Files.exists(executablePath)) {
                throw new IllegalArgumentException("The executable file does not exists. " + executablePath);
            }
            if (!Files.isReadable(executablePath)) {
                throw new IllegalArgumentException("The executable file is not readable. " + executablePath);
            }
            if (executableOnly && !Files.isExecutable(executablePath)) {
                throw new IllegalArgumentException("The executable file does not have the operating system executable permission. " + executablePath);
            }
            TextFile.FsTextFileBuilder fsTextFileBuilder = TextFile.builder(executablePath).setCharset(this.charset);
            try {
                this.shebangArguments = fsTextFileBuilder.build().getShebangArgs();
            } catch (TextDetectedCharsetNotSupported e) {
                throw new IllegalStateException("The detected character set (" + fsTextFileBuilder.getCharsetName() + ") is not supported by your OS. Error: " + e.getMessage(), e);
            } catch (TextCharacterSetNotDetected e) {
                // not a text file
            }
            if (workingDirectory == null) {
                workingDirectory = Paths.get(".");
            } else {
                if (!Files.exists(workingDirectory)) {
                    throw new IllegalArgumentException("Working directory does not exist: " + workingDirectory);
                }
                if (!Files.isDirectory(workingDirectory)) {
                    throw new IllegalArgumentException("Working directory is not a directory: " + workingDirectory);
                }
            }
            return new OsExec(this);
        }


    }
}
