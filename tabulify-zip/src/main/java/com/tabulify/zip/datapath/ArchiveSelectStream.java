package com.tabulify.zip.datapath;

import com.tabulify.model.ColumnDef;
import com.tabulify.stream.SelectStreamAbs;
import com.tabulify.zip.api.ArchiveEntry;
import com.tabulify.zip.api.ArchiveEntryAttribute;
import com.tabulify.zip.api.ArchiveIterator;
import net.bytle.type.Casts;


public class ArchiveSelectStream extends SelectStreamAbs {
  private ArchiveIterator archiveIterator;
  private ArchiveEntry entry;
  private int recordId = 0;

  public ArchiveSelectStream(ArchiveDataPath archiveDataPath) {
    super(archiveDataPath);
    this.archiveIterator = this.buildArchiveIterator();
  }

  private ArchiveIterator buildArchiveIterator() {
    ArchiveDataPath archiveDataPath = (ArchiveDataPath) getDataPath();
    return ArchiveIterator.builder()
      .setArchive(archiveDataPath.getArchive())
      .setNameSelector(archiveDataPath.getNameSelector())
      .build();
  }

  @Override
  public boolean next() {
    boolean hasNext = this.archiveIterator.hasNext();

    if (!hasNext) {
      entry = null;
      return false;
    }
    recordId++;
    entry = this.archiveIterator.next();
    return true;
  }

  @Override
  public void close() {

    archiveIterator.close();

  }


  @Override
  public boolean isClosed() {
    return archiveIterator.isClosed();
  }

  @Override
  public long getRecordId() {
    return recordId;
  }

  @Override
  public Object getObject(ColumnDef columnDef) {

    ArchiveEntryAttribute name = Casts.castSafe(columnDef.getColumnNameNormalized(), ArchiveEntryAttribute.class);
    return this.entry.getValueFromAttribute(name);

  }

  @Override
  public void beforeFirst() {
    recordId = 0;
    close();
    this.archiveIterator = this.buildArchiveIterator();
  }

}
