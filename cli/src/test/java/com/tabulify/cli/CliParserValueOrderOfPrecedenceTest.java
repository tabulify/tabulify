package com.tabulify.cli;

import com.tabulify.type.env.OsEnvs;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CliParserValueOrderOfPrecedenceTest {

    /**
     * Test the order of precedence
     * for the options.
     * <p>
     * 1 - first command line
     * 2 - then system property (normally only the config file location)
     * 3 - then environment variable
     * 4 - then config property (yaml)
     * 5 - then default value
     */
    @Test
    public void optionValueOrderOfPrecedence() {


        String[] args = {};
        CliCommand cli = CliCommand.createRoot("cli", args);

        String ENV_VAR_NAME = "CLI_INPUT";
        String WORD_NAME = "--single.value";

        String defaultValue = "defaultValue";
        cli.addProperty(WORD_NAME)
                .setDefaultValue(defaultValue);

        // First default
        CliParser cliParser = cli.parse();
        String inputValue = cliParser.getString(WORD_NAME);
        Assert.assertEquals("The default value must be found", defaultValue, inputValue);

        // Value inf the config
        Map<String, Object> conf = new HashMap<>();
        String configExpectedValue = "configValue";
        conf.put(WORD_NAME, configExpectedValue);
        cli.addConfigurations(conf);
        inputValue = cliParser.getString(WORD_NAME);
        Assert.assertEquals("The value in the config file must be found", configExpectedValue, inputValue);

        // The value found is the value of the environment variable
        cli.addProperty(WORD_NAME)
                .setEnvName(ENV_VAR_NAME);
        String env_var_value = "3";
        cli.setOsEnv(Map.of(ENV_VAR_NAME, env_var_value));
        cliParser = cli.parse(args);
        inputValue = cliParser.getString(WORD_NAME);
        Assert.assertEquals("the value in the configuration must be found", env_var_value, inputValue);


        // The value found in the system property
        String systemPropertyValue = "systemPropertyValue";
        String sysPropName = "SYS_PROP_NAME";
        cli.addProperty(WORD_NAME).setSystemPropertyName(sysPropName);
        System.setProperty(sysPropName, systemPropertyValue);
        cliParser = cli.parse();
        inputValue = cliParser.getString(WORD_NAME);
        Assert.assertEquals("The value in the system property must be found", systemPropertyValue, inputValue);


        // The value found is the value passed in the arguments
        String[] args1 = {WORD_NAME, "4"};
        cliParser = cli.parse(args1);
        inputValue = cliParser.getString(WORD_NAME);
        Assert.assertEquals("The value found is the value on the command line arguments and not the config file", "4", inputValue);

        System.clearProperty(sysPropName);
    }
}
