package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;
import static com.tabulify.tabul.TabulWords.INIT_COMMAND;


public class TabulApp {


    public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

        childCommand.setDescription("Management app");

        childCommand.addChildCommand(TabulWords.INIT_COMMAND)
                .setDescription("Init a new app");


        CliParser cliParser = childCommand.parse();
        List<DataPath> feedbackDataPaths = new ArrayList<>();

        List<CliCommand> commands = cliParser.getFoundedChildCommands();
        if (commands.isEmpty()) {
            throw new IllegalCommandException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
        }
        for (CliCommand subChildCommand : commands) {
            LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
            switch (subChildCommand.getName()) {
                case INIT_COMMAND:
                    feedbackDataPaths = TabulAppInit.run(tabular, subChildCommand);
                    break;
                default:
                    throw new IllegalCommandException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
            }

        }
        return feedbackDataPaths;
    }


}
