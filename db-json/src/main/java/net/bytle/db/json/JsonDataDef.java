package net.bytle.db.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.textline.LineDataDef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonDataDef extends LineDataDef {

  private final JsonDataPath jsonDataPath;
  private boolean columnsWereBuild = false;

  public JsonDataDef(JsonDataPath dataPath) {
    super(dataPath);
    this.jsonDataPath = dataPath;
  }

  @Override
  public ColumnDef[] getColumnDefs() {
    buildColumnNamesIfNeeded();
    return super.getColumnDefs();
  }

  @Override
  public <T> ColumnDef<T> getColumnDef(String columnName) {
    buildColumnNamesIfNeeded();
    return super.getColumnDef(columnName);
  }

  @Override
  public <T> ColumnDef<T> getColumnDef(Integer columnIndex) {
    buildColumnNamesIfNeeded();
    return super.getColumnDef(columnIndex);
  }

  @Override
  public JsonDataPath getDataPath() {
    return jsonDataPath;
  }

  private void buildColumnNamesIfNeeded() {
    if (super.getColumnDefs().length == 0 && !this.columnsWereBuild) {
      this.columnsWereBuild=true;
      try {
        JsonFactory jsonFactory = new JsonFactory();
        Path nioPath = jsonDataPath.getNioPath();
        if (!Files.exists(nioPath)){
          throw new RuntimeException("The file "+nioPath+" does not exist, we can't read it");
        }
        Files.newBufferedReader(nioPath).lines().forEach(
          s-> {
            try {
              JsonParser jsonParser = jsonFactory.createParser(s);
              // Sanity check: verify that we got "Json Object":
              if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected data to start with an Object");
              }

              // Iterate over object fields:
              while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                // In the while, we moved to the field name
                addColumn(jsonParser.getCurrentName());
                // Let's move to value
                jsonParser.nextToken();
              }

              jsonParser.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        );
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
