package net.bytle.db.engine;

import java.util.List;

public interface Relational<T> {

  abstract List<T> getParents();

}
