package net.bytle.email.flow.flow;

import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.flow.engine.StepProvider;

import java.util.HashSet;
import java.util.Set;

public class SendmailStepProvider extends StepProvider {


  @Override
  public Boolean accept(String commandName) {
    return SendmailStep.STEP_NAME.equalsIgnoreCase(commandName.trim());
  }

  @Override
  public Set<String> getAcceptedCommandNames() {
    Set<String> sets = new HashSet<>();
    sets.add(SendmailStep.STEP_NAME);
    return sets;
  }

  @Override
  public OperationStep createStep() {
    return new SendmailStep();
  }

}
