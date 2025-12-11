package com.tabulify.command;


import com.tabulify.exec.Command;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcessTest {

    @Test
    public void baselineTest() {

        Path workingDirectory = Paths.get("./target");
        String command = "echo";

        Command bprocess = Command.create(command)
                .addArg("hello")
                .addArg("nico")
                .setWorkingDirectory(workingDirectory);

        bprocess.startAndWait();

        int exitValue = bprocess.getExitValue();
        String output = bprocess.getOutput();

        Assert.assertEquals("The exit value is 0", 0, exitValue);
        Assert.assertEquals("The outputStream", "hello nico\n", output);

    }

}
