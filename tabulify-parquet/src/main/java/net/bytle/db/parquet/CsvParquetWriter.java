package net.bytle.db.parquet;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.schema.MessageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvParquetWriter {

  /**
   * Creates a Builder for configuring ParquetWriter with the example object
   * model.
   *
   * @param file the output file to create
   * @return a {@link Builder} to create a {@link ParquetWriter}
   */
  public static Builder builder(Path file) {
    return new Builder(file);
  }


  /**
   * This object is returned from the above Builder constructor
   * and therefore must be public
   */
  public static class Builder extends ParquetWriter.Builder<List<String>, Builder> {

    private MessageType type = null;
    private Map<String, String> extraMetaData = new HashMap<String, String>();

    protected Builder(Path path) {
      super(path);
    }


    public Builder withType(MessageType type) {
      this.type = type;
      return this;
    }

    public Builder withExtraMetaData(Map<String, String> extraMetaData) {
      this.extraMetaData = extraMetaData;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }


    @Override
    protected WriteSupport<List<String>> getWriteSupport(Configuration configuration) {
      return new CsvWriteSupport(type);
    }

  }
}
