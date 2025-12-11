package com.tabulify.csv;

import com.tabulify.diff.DataDiffEqualityType;
import com.tabulify.diff.DataDiffReportAccumulator;
import com.tabulify.diff.DataPathDiff;
import com.tabulify.diff.DataPathDiffResult;
import com.tabulify.fs.FsConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.transfer.TransferManager;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferPropertiesSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class CsvTransfer extends CsvBase {

  /**
   * Insert from one csv to a non-existent target with header
   */
  @Test
  public void transferInsertOneToOneWithHeaderNonExistentTarget() {

    FsConnection tmpConnection = tabular.getTmpConnection();

    CsvDataPath source = (CsvDataPath) ((CsvDataPath) tmpConnection.getDataPath("prefix-first.csv"))
      .setHeaderRowId(1)
      .createEmptyRelationDef()
      .addColumn("int", Integer.class)
      .setPrimaryKey("int")
      .getDataPath();

    Tabulars.dropIfExists(source);
    Assertions.assertFalse(Tabulars.exists(source));
    Tabulars.create(source);
    try (InsertStream insertStream = source.getInsertStream()) {
      insertStream.insert(1);
    }

    String targetUri = "target.csv";
    DataPath target = tmpConnection.getDataPath(targetUri);
    Tabulars.dropIfExists(target);
    Assertions.assertFalse(Tabulars.exists(target));

    TransferManager.builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem.builder()
          .setOperation(TransferOperation.INSERT)
      )
      .build()
      .createOrder(source, target)
      .execute();


    Assertions.assertTrue(Tabulars.exists(target));
    Assertions.assertEquals(1, target.getCount());

    // Visual Test
    // Tabulars.print(target);

  }

  @Test
  public void transferConcatWithHeaderNonExistingTarget() {

    FsConnection tmpConnection = tabular.getTmpConnection();
    List<String> names = List.of("prefix-first.csv", "prefix-second.csv");
    List<CsvDataPath> sources = new ArrayList<>();
    for (String name : names) {
      CsvDataPath source = (CsvDataPath) ((CsvDataPath) tmpConnection.getDataPath(name))
        .setHeaderRowId(1)
        .createEmptyRelationDef()
        .addColumn("int", Integer.class)
        .setPrimaryKey("int")
        .getDataPath();
      sources.add(source);
      Tabulars.dropIfExists(source);
      Assertions.assertFalse(Tabulars.exists(source));
      Tabulars.create(source);
      try (InsertStream insertStream = source.getInsertStream()) {
        insertStream.insert(1);
      }
      Tabulars.print(source);
    }


    String targetUri = "target.csv";
    DataPath target = tmpConnection.getDataPath(targetUri);
    Tabulars.dropIfExists(target);
    Assertions.assertFalse(Tabulars.exists(target));


    TransferManager.builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem.builder()
          .setOperation(TransferOperation.INSERT)
      )
      .build()
      .createOrder(sources, target)
      .execute();

    Assertions.assertTrue(Tabulars.exists(target));
    Assertions.assertEquals(2, target.getCount());
    // Data Diff
    List<List<?>> expectedData = List.of(List.of(1), List.of(1));
    DataPathDiffResult diffResult = DataPathDiff
      .builder(tabular)
      .setReportAccumulator(DataDiffReportAccumulator.UNIFIED)
      .setEqualityType(DataDiffEqualityType.LOSS)
      .build()
      .diff(expectedData, target);

    // Visual test
    // Tabulars.print(diffResult.getResultAccumulatorReport());

    Assertions.assertTrue(diffResult.areEquals());

    // Visual test
    // Tabulars.print(target);

  }

}
