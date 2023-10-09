package net.bytle.smtp;

import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * A type of {@link SmtpInput}
 * which is a command line.
 * A {@link SmtpCommand} with its arguments
 */
public class SmtpInputCommand {

  private final List<String> arguments;
  private final SmtpCommand command;


  /**
   * We build and validate after the creation
   * to be able to build the {@link SmtpSessionInteractionHistory}
   */
  public SmtpInputCommand(SmtpInput smtpInput) throws SmtpException {


    if (smtpInput.getSession().getTransactionState().isDataReceptionMode()) {
      /**
       * Data Line, the returned command (ie state)
       * is the actual state with no arguments
       */
      command = smtpInput.getSession().getTransactionState().getState();
      arguments = new ArrayList<>();
      return;
    }

    /**
     * Command line
     */
    List<String> commandWords = Strings.createFromString(smtpInput.getLine())
      .split(" ");
    if (commandWords.size() == 0) {
      throw SmtpException.createBadSyntax("A command was expected");
    }
    String commandString = commandWords.get(0);
    this.arguments = commandWords.subList(1, commandWords.size());
    try {
      this.command = Casts.cast(commandString, SmtpCommand.class);
    } catch (CastException e) {
      throw SmtpException.create(SmtpReplyCode.NOT_RECOGNIZED_500, "The command " + commandString + ") is unknown");
    }

  }

  public List<String> getArguments() {
    return this.arguments;
  }

  public SmtpCommand getCommand() {
    return this.command;
  }


}
