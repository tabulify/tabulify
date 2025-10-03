package com.tabulify.memory;

import com.tabulify.memory.list.MemoryListDataPath;
import com.tabulify.memory.queue.MemoryQueueDataPath;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystemAbs;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.*;
import net.bytle.exception.InternalException;
import net.bytle.regexp.Glob;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.transfer.TransferOperation.COPY;


/**
 * No data in their please
 */
public class MemoryDataSystem extends DataSystemAbs {


  private final MemoryConnection memoryConnection;

  public MemoryDataSystem(MemoryConnection memoryConnection) {
    super(memoryConnection);
    this.memoryConnection = memoryConnection;
  }


  @Override
  public void truncate(List<DataPath> dataPaths, Set<DropTruncateAttribute> attributes) {
    ((MemoryDataPath) dataPaths).truncate();
  }

  @Override
  public MediaType getContainerMediaType() {
    throw new UnsupportedOperationException("Memory system does not support container resources (directory)");
  }


  public InsertStream getInsertStream(TransferSourceTargetOrder transferSourceTarget) {

    return transferSourceTarget.getTargetDataPath().getInsertStream();

  }

  @SuppressWarnings("unchecked")
  @Override
  public List<MemoryDataPath> getChildrenDataPath(DataPath dataPath) {

    /**
     * We don't throw because {@link com.tabulify.sample.BytleSchema}
     * is multi connection
     */
    return new ArrayList<>();


  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {

    return dataPath.getCount() == 0;

  }


  @Override
  public boolean isDocument(DataPath dataPath) {
    return !dataPath.equals(dataPath.getConnection().getCurrentDataPath());
  }


  @Override
  public String getContentAsString(DataPath dataPath) {
    int columnSize = dataPath.getOrCreateRelationDef().getColumnsSize();
    StringBuilder string = new StringBuilder();
    try (SelectStream selectString = dataPath.getSelectStream()) {
      while (selectString.next()) {
        if (string.length() > 0) {
          string.append(Strings.EOL);
        }
        for (int i = 0; i < columnSize; i++) {
          string.append(selectString.getObject(i + 1));
        }
      }
    } catch (SelectException e) {
      throw new InternalException("Select stream memory error: ", e);
    }
    return string.toString();
  }

  @Override
  public TransferListener transfer(TransferSourceTargetOrder transferOrder) {

    DataPath source = transferOrder.getSourceDataPath();
    DataPath target = transferOrder.getTargetDataPath();
    TransferPropertiesSystem transferProperties = transferOrder.getTransferProperties();

    TransferListener transferListenerStream = new TransferListenerAtomic(transferOrder)
      .startTimer();
    RelationDef targetRelationDef = target.getRelationDef();
    if (targetRelationDef == null) {
      target
        .mergeDataDefinitionFrom(source);
    }
    if (source instanceof MemoryQueueDataPath) {
      throw new UnsupportedOperationException("The memory transfer with the type " + source.getClass().getSimpleName() + " is not yet implemented");
    }
    if (source instanceof MemoryListDataPath) {
      MemoryListDataPath sourceMemoryList = (MemoryListDataPath) source;
      List<List<?>> values = sourceMemoryList.getValues();
      if (!(target instanceof MemoryListDataPath)) {
        throw new UnsupportedOperationException("The memory transfer to the target type " + target.getClass().getSimpleName() + " is not yet implemented");
      }
      MemoryListDataPath targetMemoryList = (MemoryListDataPath) target;
      List<List<?>> targetValues = ((MemoryListDataPath) target).getValues();
      TransferOperation operation = transferProperties.getOperation();
      switch (operation) {
        case COPY:
        case INSERT:
          if (operation.equals(COPY) && !targetValues.isEmpty()) {
            throw new IllegalArgumentException("The target (" + target + ") is not empty. The copy operation does not allow that.");
          }
          if (targetValues.isEmpty()) {
            targetMemoryList.setValues(values);
            break;
          }
          if (transferProperties.getRunPreDataOperation()
            && transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP)) {
            targetMemoryList.setValues(values);
            break;
          }
          targetMemoryList.addValues(values);
          break;
        default:
          throw new UnsupportedOperationException("The transfer operation " + operation + " is not yet implemented in the memory system");
      }
      transferListenerStream.stopTimer();
      return transferListenerStream;

    }
    throw new InternalException("The memory transfer with the type " + source.getClass().getSimpleName() + " should be processed");

  }


  @SuppressWarnings("unchecked")
  @Override
  public List<MemoryDataPath> getDescendants(DataPath dataPath) {

    /**
     * Only one level for now
     */
    return getChildrenDataPath(dataPath);

  }

  @SuppressWarnings("unchecked")
  @Override
  public List<MemoryDataPath> select(DataPath dataPath, String patternOrPath, MediaType mediaType) {

    return getChildrenDataPath(dataPath)
      .stream()
      .filter(dp -> Glob.createOf(patternOrPath).matches(dp.getName()))
      .collect(Collectors.toList());
  }

  @Override
  public List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath) {
    return List.of();
  }

  @Override
  public void dropConstraint(Constraint constraint) {
    throw new RuntimeException("The memory system does not have the notion of constraint yet");
  }


  @Override
  public MemoryConnection getConnection() {
    return memoryConnection;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {

    return false;

  }

  /**
   * A memory may be created after it's configuration
   * A queue for instance needs a capacity
   */
  @Override
  public void create(DataPath dataPath, DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargets) {

    // Create the structure
    ((MemoryDataPath) dataPath).create();

  }

  @Override
  public void drop(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {

    // We don't send an error otherwise transfer becomes a nightmare
    // No store, dropping succeed

  }


  @Override
  public Boolean exists(DataPath dataPath) {
    // Transfer will check if the source exists
    return true;
  }

}
