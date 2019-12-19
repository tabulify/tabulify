/*
 * Copyright (c) 2014. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package net.bytle.db.resultSetDiff;


import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Created by gerard on 6/16/2014.
 * All function to perform a data set diff
 */
public class DataSetDiff {


    private static final int LOSS_EQUALITY = 0;
    private static final int STRICT_EQUALITY = 1;

    private static final String EQUAL = "";
    private static final String PLUS = "+";
    private static final String MIN = "-";
    private static final Logger LOGGER = Logger.getLogger(DataSetDiff.class.getPackage().toString());
    ;

    private final DataPath sourceDataPath;
    private final DataPath targetDataPath;
    private final Integer keyColumnIndex; // The index of the key column
    private final Locale locale; // The locale used for String comparison

    private DataPath diffDataPath; // will contains the diff result
    private StringBuilder consoleMessage = new StringBuilder(); // will contains the console message

    // Represent the type of record inserted in the result
    private final int SOURCE_TYPE = 0;
    private final int TARGET_TYPE = 1;
    private Boolean dataSetDiffFound = false;
    private int rowInDifFile = 0; // To feedback the number of row in the file
    private int counterDiffLoop = 0; // To Feeback the number of loop performed

    // Global for procecssing
    private SelectStream selectSourceStream;
    private InsertStream diffInsertStream;
    private SelectStream selectTargetStream;

    public DataSetDiff(DataPath sourceDataPath, DataPath targetDataPath, Integer keyColumnIndex, Path outputPath) {

        this.sourceDataPath = sourceDataPath;
        this.targetDataPath = targetDataPath;
        this.keyColumnIndex = keyColumnIndex;
        this.diffDataPath = DataPaths.of(outputPath);
        this.locale = Locale.getDefault();


    }

    /*
     * DataSet Diff without a primary key column
     */
    public DataSetDiff(DataPath sourceDataPath, DataPath targetDataPath) {

        this(sourceDataPath, targetDataPath, null, null);

    }

    /*
     * Compare the current data set with the data set in the parameter
     * and return a data set that contains the differences.
     * If the data set is empty the data set are equals.
     * Throws an exception if the data set are not comparable with the reason
     * The actual data set is the source and the data set given in the parameters is the target
     * @param key: the key column. A key column can only be ordinal (then of type Numeric (Date) or String)
     * The key column can be null, in this case, the row num is considered as the key
     * The key column can not be null, otherwise a DataSet exception is thrown
     * The data set key values are supposed to be in an Ascendant order
     */
    public Boolean diff() throws SQLException {

        // Have the data sets the same structure
        String reason = compareMetaData(sourceDataPath, targetDataPath);

        // Inserting the data

        diffInsertStream = Tabulars.getInsertStream(diffDataPath);
        selectSourceStream = Tabulars.getSelectStream(sourceDataPath);
        selectTargetStream = Tabulars.getSelectStream(targetDataPath);

        // Can we diff the data set
        if (!reason.equals("")) {

            this.setDataSetDiffFound(true);
            diffInsertStream.insert(reason);


        } else {

            // Add a column in the first position with a type string
            List csvRow = new ArrayList();
            csvRow.add(0, "+/-");
            // Add the key column in the second position with the key type
            csvRow.add(1, "Key");

            // The headers
            TableDef sourceDataDef = sourceDataPath.getDataDef();
            for (int i = 1; i <= sourceDataDef.getColumnDefs().size(); i++) {
                csvRow.add(sourceDataDef.getColumnDef(i).getColumnName());
            }

            diffInsertStream.insert(csvRow);

            // Should we of the next row for the source or the target
            Boolean sourceNext = true;
            Boolean targetNext = true;

            // Does the source and target set has still row
            Boolean moreSourceRow = false;
            Boolean moreTargetRow = false;

            // No key, will come
            // We break the loop
            while (true) {

                this.counterDiffLoop++;

                if (sourceNext) {
                    moreSourceRow = selectSourceStream.next();
                }
                if (targetNext) {
                    moreTargetRow = selectTargetStream.next();
                }

                if (!moreSourceRow && !moreTargetRow) {
                    // No more rows to process
                    break;
                } else if (moreSourceRow && moreTargetRow) {

                    if (keyColumnIndex != null) {

                        Object keySourceValue = selectSourceStream.getObject(keyColumnIndex);
                        Object keyTargetValue = selectTargetStream.getObject(keyColumnIndex);

                        if (keySourceValue == null || keyTargetValue == null) {

                            String typeDataSet;
                            SelectStream dataset;
                            if (keySourceValue == null) {
                                dataset = selectSourceStream;
                                typeDataSet = "source";
                            } else {
                                dataset = selectTargetStream;
                                typeDataSet = "target";
                            }
                            throw new RuntimeException("The value of the primary key column (col: " + keyColumnIndex + ", row:" + dataset.getRow() + ") in the " + typeDataSet + " data set can not be null");
                        }

                        if (keySourceValue.equals(keyTargetValue)) {

                            compareAndAddRowData();
                            targetNext = true;
                            sourceNext = true;

                        } else if (keySourceValue.toString().compareTo(keyTargetValue.toString()) > 0) {

                            addRowData(TARGET_TYPE);
                            targetNext = true;
                            sourceNext = false;

                        } else {

                            //less than
                            addRowData(SOURCE_TYPE);
                            targetNext = false;
                            sourceNext = true;

                        }


                    } else {

                        // No primary key
                        compareAndAddRowData();
                        targetNext = true;
                        sourceNext = true;

                    }
                } else if (moreSourceRow == false || moreTargetRow == false) {
                    // There is still a source row of a target row
                    if (moreSourceRow) {

                        //addRowData(SOURCE_TYPE);
                        targetNext = false;
                        sourceNext = true;

                    } else {

                        //addRowData(TARGET_TYPE);
                        targetNext = true;
                        sourceNext = false;

                    }
                }
            }
        }

        // Close
        diffInsertStream.close();
        selectSourceStream.close();
        selectTargetStream.close();

        LOGGER.info("Counter info: number of row in the diff file: (" + this.rowInDifFile + ")");
        LOGGER.info("Counter info: number of diff loop: (" + this.counterDiffLoop + ")");
        return this.dataSetDiffFound;

    }

    private void addRowData(int type) throws SQLException {
        addRowData(type, null, null);
    }

    private void addRowData(int type, String equalMinPlus, List<Diff> columnPositionWithDiff) throws SQLException {

        if (diffDataPath != null) {

            this.rowInDifFile++;

            SelectStream dataSet;
            if (type == SOURCE_TYPE) {
                dataSet = selectSourceStream;
                if (equalMinPlus == null) {
                    equalMinPlus = MIN;
                }

            } else {
                dataSet = selectTargetStream;
                if (equalMinPlus == null) {
                    equalMinPlus = PLUS;
                }
            }


            List csvRow = new ArrayList();

            // Min Plus Column
            csvRow.add(equalMinPlus);

            // Key Column
            if (keyColumnIndex != null) {
                csvRow.add(keyColumnIndex);
            } else {
                csvRow.add(dataSet.getRow());
            }

            List<Integer> columnPositions = new ArrayList<>();
            if (columnPositionWithDiff != null) {
                columnPositionWithDiff.forEach(diff -> columnPositions.add(diff.getPosition()));
            }

            for (int i = 1; i <= dataSet.getSelectDataDef().getColumnDefs().size(); i++) {
                Object object = dataSet.getObject(i);

                if (columnPositions.contains(i)) {
                    if (object != null) {
                        object = "***" + object + "***";
                    } else {
                        object = "***(null)***";
                    }
                }

                csvRow.add(object);
            }

            diffInsertStream.insert(csvRow);


        }


    }

    /*
     * Compare a whole row and
     */
    private void compareAndAddRowData() throws SQLException {
        Boolean diffFound = false;
        List<Diff> columnPositionWithDiff = new ArrayList<>();
        for (int i = 1; i <= selectSourceStream.getSelectDataDef().getColumnDefs().size(); i++) {

            String cellCoordinates = "Cell(Row,Col)(" + selectSourceStream.getRow() + "," + i + ")";

            Object sourceDataPoint = selectSourceStream.getObject(i);
            Object targetDataPoint = selectTargetStream.getObject(i);
            if (sourceDataPoint != null) {
                if (targetDataPoint == null) {
                    diffFound = true;
                } else {
                    if (!sourceDataPoint.equals(targetDataPoint)) {

                        diffFound = true;
                        Diff diff = new Diff(i);
                        if (sourceDataPoint.getClass().equals(Double.class)) {
                            boolean equalWithFloat = (new Float((Double) sourceDataPoint).equals(new Float((Double) targetDataPoint)));
                            if (equalWithFloat) {
                                diff.setType(LOSS_EQUALITY);
                                diffFound = false;
                            }
                        }
                        columnPositionWithDiff.add(diff);

                        LOGGER.fine(cellCoordinates + ", Diff Found !: " + sourceDataPoint + "," + targetDataPoint);

                    } else {

                        LOGGER.fine(cellCoordinates + ", No Diff: " + sourceDataPoint + "," + targetDataPoint);

                    }
                }
            } else {
                if (targetDataPoint != null) {
                    diffFound = true;
                    LOGGER.fine(cellCoordinates + ", Diff: (null)," + targetDataPoint);
                } else {
                    LOGGER.fine(cellCoordinates + ", No Diff: (null),(null)");
                }
            }

        }

        if (diffFound) {
            addRowData(SOURCE_TYPE, null, columnPositionWithDiff);
            addRowData(TARGET_TYPE, null, columnPositionWithDiff);
            this.setDataSetDiffFound(diffFound);
        } else {
            addRowData(SOURCE_TYPE, EQUAL, null);
        }

    }

    //
    // If the metadata are equals, this function return null. Otherwise return the reason
    //
    static public String compareMetaData(DataPath sourceDataSet, DataPath targetDataSet) throws SQLException {

        StringBuilder reason = new StringBuilder();

        // Length
        int sourceSize = sourceDataSet.getDataDef().getColumnDefs().size();
        int targetSize = targetDataSet.getDataDef().getColumnDefs().size();
        if (sourceSize != targetSize) {
            reason.append("The number of columns are not equals. The source data set has " + sourceSize + " columns, and the target data set has " + targetSize + " columns." + System.getProperty("line.separator"));
        }


        // Type
        for (int i = 1; i <= sourceSize; i++) {
            ColumnDef sourceColumn = sourceDataSet.getDataDef().getColumnDef(i);
            ColumnDef targetColumn = targetDataSet.getDataDef().getColumnDef(i);
            if (sourceColumn.getDataType().getTypeCode() != targetColumn.getDataType().getTypeCode()) {
                reason.append("The type column of the column (" + i + ") are not equals. The source column (" + sourceColumn.getColumnName() + ") has the type (" + sourceColumn.getDataType().getTypeName() + ") whereas the target column (" + targetColumn.getColumnName() + ") has the column type (" + targetColumn.getDataType().getTypeName() + ")." + System.getProperty("line.separator"));
            }
        }
        return reason.toString();

    }


    public void setDataSetDiffFound(Boolean dataSetDiffFound) {
        this.dataSetDiffFound = dataSetDiffFound;
    }

    private class Diff {

        private Integer position;
        private Integer type = STRICT_EQUALITY;

        public Diff(int position) {
            this.position = position;
        }


        void setType(Integer type) {
            this.type = type;
        }

        public Integer getType() {
            return type;
        }

        public Integer getPosition() {
            return position;
        }
    }

}
