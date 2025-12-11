package com.tabulify.exec;

import com.tabulify.fs.Fs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class OsExecTest {

  @Test
  void nonExecutableBashScriptToMemory() {
    Path script = Paths.get("src/test/resources/exec/shell-script");
    ProcessResult shebangExec = OsExec.builder()
      .setExecutablePath(script)
      .setExecutableOnly(false)
      .build()
      .execute();
    Assertions.assertEquals("Hello World", shebangExec.getOutput().getString().trim());
  }

  @Test
  void nonExecutableBashScriptToOutputStream() throws IOException {
    Path output = Files.createTempFile("os-exec", ".txt");
    Path script = Paths.get("src/test/resources/exec/shell-script");
    ProcessResult shebangExec = OsExec.builder()
      .setExecutablePath(script)
      .setExecutableOnly(false)
      .setStandardOutputPath(output)
      .build()
      .execute();
    ProcessOutput processOutput = shebangExec.getOutput();
    Assertions.assertEquals("Hello World", processOutput.getString().trim());
    Assertions.assertTrue(Files.exists(output));
    String content = Fs.getFileContent(output);
    Assertions.assertEquals("Hello World", content.trim());
  }

  @Test
  void nonExecutableBashScriptToErrStream() throws IOException {
    Path output = Files.createTempFile("os-exec", ".txt");
    Path error = Files.createTempFile("os-exec", ".txt");
    Path script = Paths.get("src/test/resources/exec/shell-script");
    ProcessResult shebangExec = OsExec.builder()
      .setExecutablePath(script)
      .setExecutableOnly(false)
      .setStandardOutputPath(output)
      .setStandardErrorPath(error)
      .build()
      .execute();
    ProcessOutput processOutput = shebangExec.getOutput();
    Assertions.assertEquals("Hello World", processOutput.getString().trim());
    Assertions.assertTrue(Files.exists(output));
    String content = Fs.getFileContent(output);
    Assertions.assertEquals("Hello World", content.trim());
    Assertions.assertTrue(Files.exists(error));
    content = Fs.getFileContent(error);
    Assertions.assertEquals("Hello Error", content.trim());
  }
}
