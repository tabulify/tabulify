package net.bytle.db.memory.list;

import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.memory.MemoryVariableManagerProvider;

public class MemoryListManagerProvider extends MemoryVariableManagerProvider {

  static private MemoryListManager memoryListManager;

  @Override
  public Boolean accept(String type) {
    return type.equals(MemoryListDataPath.TYPE);
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    if (memoryListManager == null) {
      memoryListManager = new MemoryListManager();
    }
    return memoryListManager;
  }

}
