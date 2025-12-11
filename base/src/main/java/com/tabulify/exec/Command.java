package com.tabulify.exec;

import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The first experimentation with Pure Java
 * We use a library now, see {@link OsExec}
 * @deprecated
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class Command {


  private final List<String> commandAndArgs = new ArrayList<>();
  private Path workingDirectory = Paths.get("");
  private Process process;

  public Command(String command) {

    commandAndArgs.add(command);

  }

  public static Command create(String command) {

    return new Command(command);
  }

  public Command addArg(String arg) {
    String[] args = arg.split(" ");
    this.commandAndArgs.addAll(Arrays.asList(args));
    return this;
  }

  public Command setWorkingDirectory(Path workingDirectory) {
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

  public void startAndWait() {
    try {
      /*
       * The problem with running interactive programs, such as sudo, from Runtime.exec is
       * that it attaches their stdin and stdout to pipes rather than the console device they need.
       * https://jonisalonen.com/2012/runtime-exec-with-unix-console-programs/
       */
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
   * @return the output and error stream
   */
  public String getOutput() {
    InputStream input = process.getInputStream();
    InputStream inputErr = process.getErrorStream();
    try {
      return Casts.cast(input, String.class) + Casts.cast(inputErr, String.class);
    } catch (CastException e) {
      throw new RuntimeException("Error while getting the output. Error: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unused")
  public Command addArgs(List<String> args) {
    for (String arg : args) {
      addArg(arg);
    }
    return this;
  }
}
