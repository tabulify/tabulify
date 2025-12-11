package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliWordGlobalTest {


    /**
     * When a global word is added and then used by a cli
     * it should be recognize
     */
    @Test
    public void basisGlobalWord() {

        CliCommand cliCommand = CliCommand.createRootWithEmptyInput("test");

        String helpWord = "--help";
        String shortName = "-h";
        CliWord globalWord = cliCommand.addWordToLibrary(helpWord)
                .setTypeAsProperty()
                .setShortName(shortName)
                .setDescription("Print this help");

        String actualShortName = cliCommand.getOrCreateWordOf(helpWord).getShortName();
        Assert.assertEquals("The global Word has been found", shortName, actualShortName);

        CliWord actualWord = cliCommand.getOrCreateWordOf(helpWord);
        Assert.assertEquals("The words must be the same", actualWord, globalWord);


        // Adding the same must not add a global word but change it
        cliCommand.addWordToLibrary(helpWord);

        Assert.assertEquals("The total number of global word stay to one", 1, cliCommand.getGlobalWords().size());

        cliCommand.addWordToLibrary("--second");

        Assert.assertEquals("The total number of global word is two", 2, cliCommand.getGlobalWords().size());
    }

}
