package com.tabulify.flow.engine;

import com.tabulify.spi.DataPath;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

public interface FilterRunnable extends RunnableFuture<Set<DataPath>> {


  void addInput(Set<DataPath> inputs);

  @Override
  void run();

  @Override
  Set<DataPath> get() throws InterruptedException, ExecutionException;



}
