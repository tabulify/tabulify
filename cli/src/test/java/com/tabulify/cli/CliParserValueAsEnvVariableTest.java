package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CliParserValueAsEnvVariableTest {

    @Test
    public void envVariableTest() {


        CliCommand cli = CliCommand.createRootWithEmptyInput("env");

        String wordName = "--input";
        String env_variable_name = "ENV_VARIABLE_TEST";
        cli.addProperty(wordName)
                .setEnvName(env_variable_name);


        // The value found is the value of the environment variable
        String env_variable_value = "3";
        cli.setOsEnv(Map.of(env_variable_name, env_variable_value));
        CliParser cliParser = cli.parse();
        String inputValue = cliParser.getString(wordName);
        Assert.assertEquals("the value in the env variable must be found", env_variable_value, inputValue);


    }
}
