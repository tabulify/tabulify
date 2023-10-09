package net.bytle.db.gen.memory;

import net.bytle.db.gen.GenDataPathType;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.memory.MemoryVariableManagerProvider;
import net.bytle.type.MediaType;

public class GenMemManagerProvider extends MemoryVariableManagerProvider {

  static private final MemoryVariableManager memManager = new GenMemManager();

  @Override
  public Boolean accept(MediaType type) {
    return GenDataPathType.DATA_GEN.toString().equals(type.toString());
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    return memManager;
  }

}
