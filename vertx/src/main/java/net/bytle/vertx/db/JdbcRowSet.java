package net.bytle.vertx.db;

import io.vertx.sqlclient.PropertyKind;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.desc.ColumnDescriptor;

import java.util.List;

public class JdbcRowSet implements RowSet<JdbcRow> {

  private final RowSet<Row> rowSet;
  RowIterator<JdbcRow> iterator;
  public JdbcRowSet(RowSet<Row> rowSet) {
    this.rowSet = rowSet;
    iterator = new JdbcRowIterator(rowSet.iterator());
  }

  @Override
  public RowIterator<JdbcRow> iterator() {
    return this.iterator;
  }

  @Override
  public int rowCount() {
    return rowSet.rowCount();
  }

  @Override
  public List<String> columnsNames() {
    return rowSet.columnsNames();
  }

  @Override
  public List<ColumnDescriptor> columnDescriptors() {
    return rowSet.columnDescriptors();
  }

  @Override
  public int size() {
    return rowSet.size();
  }

  @Override
  public <V> V property(PropertyKind<V> propertyKind) {
    return rowSet.property(propertyKind);
  }

  @Override
  public RowSet<JdbcRow> value() {
    return new JdbcRowSet(rowSet.value());
  }

  @Override
  public RowSet<JdbcRow> next() {
    return new JdbcRowSet(rowSet.next());
  }
}
