package net.bytle.db.memory;

/**
 * A variable manager is responsible for the instantiation of a file with a structure
 * A variable manager for a defined type can be defined via the {@link MemoryVariableManagerProvider}
 *
 */
public interface MemoryVariableManager {



  MemoryDataPath createDataPath(MemoryDataStore memoryDataStore, String path);


}
