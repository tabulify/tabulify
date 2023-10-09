package net.bytle.db.memory.list;

import net.bytle.db.memory.MemoryDataPathType;
import net.bytle.type.MediaType;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.memory.MemoryVariableManagerProvider;

public class MemoryListManagerProvider extends MemoryVariableManagerProvider {

  static private MemoryListManager memoryListManager;

  @Override
  public Boolean accept(MediaType type) {
    return type.toString().equalsIgnoreCase(MemoryDataPathType.LIST.toString());
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    if (memoryListManager == null) {
      memoryListManager = new MemoryListManager();
    }
    return memoryListManager;
  }

}
