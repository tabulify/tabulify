package com.tabulify.gen.memory;

import com.tabulify.gen.GeneratorMediaType;
import com.tabulify.memory.MemoryVariableManager;
import com.tabulify.memory.MemoryVariableManagerProvider;
import net.bytle.type.MediaType;

public class GenMemManagerProvider extends MemoryVariableManagerProvider {

  static private final MemoryVariableManager memManager = new GenMemManager();

  @Override
  public Boolean accept(MediaType type) {
    return GeneratorMediaType.DATA_GEN.toString().equals(type.toString());
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    return memManager;
  }

}
