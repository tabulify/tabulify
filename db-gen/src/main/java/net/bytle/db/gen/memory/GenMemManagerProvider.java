package net.bytle.db.gen.memory;

import net.bytle.db.gen.GenDataDef;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.memory.MemoryVariableManagerProvider;

public class GenMemManagerProvider extends MemoryVariableManagerProvider {

  static private MemoryVariableManager memManager = new GenMemManager();

  @Override
  public Boolean accept(String type) {
    return GenDataDef.TYPE.equals(type);
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    return memManager;
  }

}
