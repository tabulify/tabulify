package net.bytle.db.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Bprocess {



    private List<String> commandAndArgs = new ArrayList<>();
    private Path workingDirectory = Paths.get(".");
    private Process process;

    public Bprocess(String command) {
        commandAndArgs.add(command);
    }

    public static Bprocess get(String command) {

        return new Bprocess(command);
    }

    public Bprocess addArg(String arg) {
        String[] args = arg.split(" ");
        this.commandAndArgs.addAll(Arrays.asList(args));
        return this;
    }

    public Bprocess setWorkingDirectory(Path workingDirectory) {
        if (!Files.exists(workingDirectory)){
            try {
                Files.createDirectories(workingDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.workingDirectory = workingDirectory;
        return this;
    }

    public void startAndWait() {
        try {
            process = new ProcessBuilder(commandAndArgs)
                    .directory(this.workingDirectory.toFile())
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public int getExitValue() {
        return process.exitValue();
    }

    /**
     *
     * @return the output and error stream
     */
    public String getOutput() {
        InputStream input = process.getInputStream();
        InputStream inputErr = process.getErrorStream();
        return InputStreams.toString(input)+InputStreams.toString(inputErr);
    }

    public Bprocess addArgs(List<String> args) {
        for (String arg:args){
            addArg(arg);
        }
        return this;
    }
}
