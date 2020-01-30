package net.bytle.db.transfer;

import net.bytle.db.stream.SelectStream;

import java.util.ArrayList;
import java.util.List;

public class SelectStreamDag {


  private List<SelectStream> selectStreams = new ArrayList<>();

  public static SelectStreamDag get(List<SelectStream> selectStreams) {
    SelectStreamDag dag = SelectStreamDag.get();
    for (SelectStream selectStream : selectStreams) {
      dag.addTable(selectStream);
    }
    return dag;
  }

  private static SelectStreamDag get() {
    return new SelectStreamDag();
  }

  private SelectStreamDag addTable(SelectStream selectStream) {
    if (!this.selectStreams.contains(selectStream)) {
      this.selectStreams.add(selectStream);
    }
    return this;
  }
}
