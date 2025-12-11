package com.tabulify.diff;

import com.tabulify.spi.DataPath;

public interface DataPathDiffReport {


  DataPath getDataPath();


  void insertResultRecord(DataDiffCells cells, DataPathDiffStatus diffStatus);

}
