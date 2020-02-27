package net.bytle.db.textline;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class LineInsertStream extends InsertStreamAbs implements InsertStream {

    private final LineDataDef csvDataDef;
    private final LineDataPath lineDataPath;
  private final BufferedWriter writer;

  public LineInsertStream(LineDataPath fsDataPath) {

        super(fsDataPath);
        this.csvDataDef = fsDataPath.getOrCreateDataDef();
        this.lineDataPath = fsDataPath;
        try {
            writer = Files.newBufferedWriter(lineDataPath.getNioPath(), csvDataDef.getCharset(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static LineInsertStream of(LineDataPath lineDataPath) {

        return new LineInsertStream(lineDataPath);

    }


    /**
     * @param values - The values to insert
     * @return the {@link InsertStream} for insert chaining
     */
    @Override
    public LineInsertStream insert(List<Object> values) {
        try {
            writer.write(values.stream().map(Object::toString).collect(Collectors.joining()));
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
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * In case of parent child hierarchy
     * we can check if we need to send the data with the function nextInsertSendBatch()
     * and send it with this function
     */
    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
