package net.bytle.db.flow.step;

import net.bytle.db.flow.engine.StepAbs;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectException;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.type.Strings;
import net.bytle.type.TailQueue;

import java.util.List;
import java.util.function.Function;

/**
 * Extract the tail element of data resource into target for a size of limit
 * <p>
 * Tail is also used in test to extract
 */
public class TailFunction extends StepAbs implements Function<DataPath, DataPath> {



  /**
   * the number of element returned
   */
  private Integer limit = 10;


  public static TailFunction create() {

    return new TailFunction();

  }

  @Override
  public DataPath apply(DataPath source) {


    DataPath target = source.getConnection().getTabular().getMemoryDataStore().getDataPath("tail_" + source.getLogicalName())
      .setDescription("The last " + limit + " records of the data resource (" + source + "): ");

    target.getOrCreateRelationDef().copyStruct(source);

    // Tail
    TailQueue<List<?>> queue = new TailQueue<>(limit);

    // Collect it
    try (
      SelectStream selectStream = source.getSelectStream()
    ) {
      RelationDef dataDef = selectStream.getRuntimeRelationDef();
      if (dataDef.getColumnsSize() == 0) {
        // No row structure even at runtime
        throw new RuntimeException(Strings.createMultiLineFromStrings(
          "The data path (" + source + ") has no row structure. ",
          "To extract a tail, a row structure is needed.",
          "Tip for intern developer: if it's a text file, create a line structure (one row, one cell with one line)")
          .toString());
      }

      // Collect the tail
      while (selectStream.next()) {
        queue.add(selectStream.getObjects());
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }

    // Then insert in the target
    try (
      InsertStream insertStream = target.getInsertStream()
    ) {
      queue.forEach(insertStream::insert);
    }


    return target;

  }

  public TailFunction setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public String getOperationName() {
    return "tail";
  }




}
