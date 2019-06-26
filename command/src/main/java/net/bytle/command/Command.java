package net.bytle.command;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Command {



    private List<String> commandAndArgs = new ArrayList<>();
    private Path workingDirectory = Paths.get(".");
    private Process process;

    public Command(String command) {
        commandAndArgs.add(command);
    }

    public static Command get(String command) {

        return new Command(command);
    }

    public Command addArg(String arg) {
        String[] args = arg.split(" ");
        this.commandAndArgs.addAll(Arrays.asList(args));
        return this;
    }

    public Command setWorkingDirectory(Path workingDirectory) {
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

    public Command addArgs(List<String> args) {
        for (String arg:args){
            addArg(arg);
        }
        return this;
    }
}
