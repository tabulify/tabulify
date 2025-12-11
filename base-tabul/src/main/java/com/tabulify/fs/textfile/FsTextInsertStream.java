package com.tabulify.fs.textfile;

import com.tabulify.model.RelationDef;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamAbs;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferManager;
import com.tabulify.transfer.TransferSourceTargetOrder;
import com.tabulify.type.Strings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.fs.textfile.FsTextDataPathAttributes.DEFAULTS.EOLS;


public class FsTextInsertStream extends InsertStreamAbs implements InsertStream {


  private final BufferedWriter bufferedWriter;
  private final String endOfRecord;

  public FsTextInsertStream(FsTextDataPath fsDataPath) {

    super(fsDataPath);
    if (Arrays.equals(fsDataPath.getEndOfRecords(), EOLS)) {
      /**
       * The OS EOL character
       */
      endOfRecord = Strings.EOL;
    } else {
      /**
       * The first one
       */
      endOfRecord = fsDataPath.getEndOfRecords()[0];
    }

    try {
      Charset charset = fsDataPath.getCharset();
      bufferedWriter = Files.newBufferedWriter(getDataPath().getAbsoluteNioPath(), charset, StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


  }

  @Override
  public FsTextDataPath getDataPath() {
    return (FsTextDataPath) super.getDataPath();
  }

  public static FsTextInsertStream create(FsTextDataPath fsTextDataPath) {


    return new FsTextInsertStream(fsTextDataPath);

  }


  /**
   * @param values - The values to insert
   * @return the {@link InsertStream} for insert chaining
   */
  @Override
  public FsTextInsertStream insert(List<Object> values) {
    try {

      /**
       * Add the end of record
       */
      values = new ArrayList<>(values);
      values.add(this.endOfRecord);
      /**
       * Write
       */
      bufferedWriter.write(
        values
          .stream()
          .map(v ->
            {
              if (v == null) {
                return "";
              } else {
                return v.toString();
              }
            }
          )
          .collect(Collectors.joining()));
      this.insertStreamListener.addRows(1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Close the stream
   * ie
   * * commit the last rows
   * * close the connection
   * * ...
   */
  @Override
  public void close() {

    try {
      bufferedWriter.close();
      this.incrementBatchAndCommitToListener();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    /**
     * During the transfer {@link TransferManager#beforeTargetCreationAndPreOperations(TransferSourceTargetOrder, TransferListener)}
     * may add columns
     * <p></p>
     * We correct it here
     * <p></p>
     * If the file is passed along another transfer will not be happy
     * because it will have multiple column for only one value
     */
    RelationDef relationDef = this.dataPath.getOrCreateRelationDef();
    if (relationDef.getColumnsSize() > 1) {
      String uniqueColumnName = relationDef.getColumnDef(1).getColumnName();
      relationDef.dropAll().addColumn(uniqueColumnName);
    }

  }

  private void incrementBatchAndCommitToListener() {
    this.insertStreamListener.incrementBatch();
    this.insertStreamListener.incrementCommit();
  }

  /**
   * In case of parent child hierarchy
   * we can check if we need to send the data with the function nextInsertSendBatch()
   * and send it with this function
   */
  @Override
  public void flush() {
    try {
      bufferedWriter.flush();
      this.incrementBatchAndCommitToListener();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
