package net.bytle.vertx.db;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;

public class JdbcRowIterator implements RowIterator<JdbcRow> {
  private final RowIterator<Row> iterator;

  public JdbcRowIterator(RowIterator<Row> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return this.iterator.hasNext();
  }

  @Override
  public JdbcRow next() {
    Row next = this.iterator.next();
    if (next == null) {
      return null;
    }
    return new JdbcRow(next);
  }

}
