package com.tabulify.flow.engine;

import java.util.ArrayList;
import java.util.List;

public class LogStep extends StepAbs implements OperationStep {


  private String input;
  private final List<LogStep> subscriber = new ArrayList<>();

  public void input(String message){

    this.input = message;
  }

  public void subscribeOutput(LogStep logStep){
    this.subscriber.add(logStep);
  }

  public void run(){

    System.out.println(input);
    for (LogStep logStep: subscriber) {
      logStep.input(input+" Nico");
      logStep.run();
    }

  }


  @Override
  public String getOperationName() {
    return "log";
  }


}
