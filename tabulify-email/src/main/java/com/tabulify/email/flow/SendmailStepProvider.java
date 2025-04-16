package com.tabulify.email.flow;

import com.tabulify.flow.engine.OperationStep;
import com.tabulify.flow.engine.StepProvider;

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
