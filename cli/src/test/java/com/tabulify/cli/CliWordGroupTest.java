package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CliWordGroupTest {

    private CliCommand cliCommand;

    @Before
    public void setUp() {
      String[] args  = {};
      cliCommand =  CliCommand.createRoot ("cliName", args)
                .setDescription("A print with the use of a group");

        final String help = "--help";
        cliCommand.addFlag(help)
                .setShortName("-h")
                .setDescription("Show the help");

        final String version = "--version";
        cliCommand.addFlag(version)
                .setDescription("Show the version")
                .addGroup("test");

        final String path = "--path";
        cliCommand.addProperty(path)
                .setShortName("-p")
                .setValueName(path)
                .setGroup("Local");

        cliCommand.addProperty("--mandatory")
                .setValueName("mand")
                .setMandatory(true);

        cliCommand.getGroup("Global")
                .setImportanceLevel(2)
                .addWordOf(help)
                .addWordOf(version);


    }

    /**
     * Must show:
     * * no option
     * * no "where"
     */
    @Test
    public void baselineLevel0Test() {


      String[] args = {};
      CliCommand cliCommand = CliCommand.createRoot("baseLineLevel0", args);

        cliCommand.addFlag("--help")
                .setShortName("-h")
                .setDescription("Show the help");

        String usage = CliUsage.get(cliCommand);
        Assert.assertNotNull("The usage shows only the level and mandatory", usage);
        System.out.println(usage);


    }

    /**
     * Must show:
     * * the level 1 properties
     * * the mandatory properties
     * * the fact that there is other properties
     */
    @Test
    public void baselineLevel1AndMandatoryTest() {


        String usage = CliUsage.get(cliCommand);
        Assert.assertNotNull("The usage shows only the level and mandatory", usage);
        System.out.println(usage);


    }

    @Test
    public void baselineLevel2Test() {


        String usage = CliUsage.get(cliCommand);
        Assert.assertNotNull("The usage level 2 see all options", usage);
        System.out.println(usage);

    }
}
