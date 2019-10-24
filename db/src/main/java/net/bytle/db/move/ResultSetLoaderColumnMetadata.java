package net.bytle.db.move;

/**
 * Created by gerard on 01-02-2016.
 * A class that contains the
 * {@link java.sql.ResultSetMetaData ResultSetMetaData}
 * that the consumers will use
 *
 * We can't rely on the {@link java.sql.ResultSetMetaData ResultSetMetaData} of the original
 * query because it may be destruct when all the cursor data is read.
 * Therefore we make a copy.
 *
 */
public class ResultSetLoaderColumnMetadata {

    private final int columnTypeCode;
    private final String columnName;

    public ResultSetLoaderColumnMetadata(int columnType, String columnName) {
        this.columnTypeCode = columnType;
        this.columnName = columnName;
    }

    public int getColumnTypeCode() {
        return columnTypeCode;
    }

    public String getColumnName() {
        return columnName;

    }
}
