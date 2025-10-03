package com.tabulify.diff;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.PrinterColor;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tabulify.diff.DataPathDiffStatus.*;


/**
 * Represent a unified diff report
 * where source and target record are in the same resource
 */
public class DataPathDiffUnifiedRecordReport implements DataPathDiffReport {


  /**
   * The result stream that will accumulate the result
   */
  private final InsertStream accumulatorResultInsertStream;
  private final DataPathDiffResult diffResult;
  private final List<DataDiffColumn> diffColumns;
  /**
   * id of the record in the accumulator
   */
  private int accumulatorRecordId = 1;

  final DataPathDiff.DataPathDiffBuilder diffBuilderMeta;

  public DataPathDiffUnifiedRecordReport(DataPathDiffResult diffResult, DataPath resultDataPath) {
    this.diffResult = diffResult;
    this.diffBuilderMeta = diffResult.diffBuilderMeta;

    this.diffColumns = diffBuilderMeta.diffColumns;


    String compColumnPrefix = diffBuilderMeta.diffColumnPrefix;
    buildResultDataPathUnifiedStructure(resultDataPath, diffColumns, compColumnPrefix);
    this.accumulatorResultInsertStream = resultDataPath.getInsertStream();


  }


  /**
   * Build a unified diff report
   */
  private void buildResultDataPathUnifiedStructure(DataPath resultDataPath, List<DataDiffColumn> columnsNames, String compColumnPrefix) {

    DataPath source = diffResult.getSource();

    String terminalComment = "";
    if (this.diffResult.getBuilder().getUseColor()) {
      terminalComment = "\n - " + PrinterColor.addColorIfNotNull("red", PrinterColor.RED) + "   for deleted\n" +
        " - " + PrinterColor.addColorIfNotNull("green", PrinterColor.GREEN) + " for added\n" +
        " - " + PrinterColor.addColorIfNotNull("blue", PrinterColor.BLUE) + "  for type change (example: number as varchar)";
    }
    RelationDef relationDef = resultDataPath
      .setComment(
        "Unified Diff Report between the resources (" + source + ") and (" + diffResult.getTarget() + ")." + terminalComment
      )
      .createEmptyRelationDef();
    for (DataDiffColumn columnsName : columnsNames) {
      relationDef.addColumn(
        compColumnPrefix + columnsName.toKeyNormalizer().toSqlCase(),
        columnsName.getValueClazz()
      );
    }

    // Add the original data columns
    RelationDef sourceDataDef = source.getOrCreateRelationDef();
    for (int i = 1; i <= sourceDataDef.getColumnsSize(); i++) {
      ColumnDef<?> columnDef = sourceDataDef.getColumnDef(i);
      resultDataPath
        .getOrCreateRelationDef()
        .addColumn(
          columnDef.getColumnName(),
          columnDef.getDataType().getAnsiType(),
          columnDef.getPrecision(),
          columnDef.getScale(),
          true, // Optional because it may not exist in the target
          columnDef.getComment());
    }

  }

  enum ValueOrigin {
    SOURCE("s"),
    TARGET("t"),
    BOTH("s");

    private final String oneLetter;

    ValueOrigin(String s) {
      this.oneLetter = s;
    }

    public String oneLetter() {
      return oneLetter;
    }
  }

  /**
   * Add a row in the diff result data
   * <p>
   *
   * @param diffCells  - the coordinates of the cells with a diff
   * @param diffStatus - the type of change
   */
  public void insertResultRecord(DataDiffCells diffCells, DataPathDiffStatus diffStatus) {


    List<Object> record;
    switch (diffStatus) {
      case DELETE:
        record = this.insert(diffCells.getCells(), diffStatus, ValueOrigin.SOURCE);
        this.accumulatorResultInsertStream.insert(record);
        return;
      case ADD:
        record = this.insert(diffCells.getCells(), diffStatus, ValueOrigin.TARGET);
        this.accumulatorResultInsertStream.insert(record);
        return;
      case NO_CHANGE:
        if (this.diffBuilderMeta.reportDensity == DataDiffReportDensity.SPARSE) {
          return;
        }
        record = this.insert(diffCells.getCells(), diffStatus, ValueOrigin.BOTH);
        this.accumulatorResultInsertStream.insert(record);
        return;
      case VALUE:
        /**
         * Target insertion first so that we see the +/added first
         * (ie the actual state)
         */
        record = this.insert(diffCells.getCells(), diffStatus, ValueOrigin.TARGET);
        this.accumulatorResultInsertStream.insert(record);
        record = this.insert(diffCells.getCells(), diffStatus, ValueOrigin.SOURCE);
        this.accumulatorResultInsertStream.insert(record);
        break;
      default:
        throw new InternalException("Type of change (" + diffStatus + ") missed in the switch branch");
    }


  }

  private List<Object> insert(List<DataDiffCell> cells, DataPathDiffStatus diffStatus, ValueOrigin origin) {


    List<Object> record = new ArrayList<>();

    for (DataDiffColumn dataDiffColumn : diffColumns) {
      switch (dataDiffColumn) {
        case ID:
          /**
           * Accumulator Id
           */
          record.add(accumulatorRecordId++);
          continue;
        case CHANGE_ID:
          /**
           * Change Identifier
           */
          if (diffStatus != NO_CHANGE) {
            record.add(this.diffResult.getChangeCounter());
          } else {
            record.add(null);
          }
          continue;
        case STATUS:
          // Min Plus Column
          String unifiedSymbol;
          switch (diffStatus) {
            case NO_CHANGE:
            case ADD:
            case DELETE:
              unifiedSymbol = diffStatus.getMathematicsSymbol();
              break;
            case VALUE:
              switch (origin) {
                case TARGET:
                  unifiedSymbol = ADD.getMathematicsSymbol();
                  break;
                case SOURCE:
                default:
                  unifiedSymbol = DELETE.getMathematicsSymbol();
                  break;
              }
              break;
            default:
              throw new InternalException("Type of change (" + diffStatus + ") missed in the switch branch");
          }

          if (diffStatus == VALUE) {
            unifiedSymbol += this.diffResult.getChangeCounter();
            /**
             * Add the source record id if there is no driver columns
             * (ie the the number of driver columns is the same as the source)
             */
            if (this.diffResult.getDriverColumnPositions().size() == diffResult.getSource().getOrCreateRelationDef().getColumnDefs().size()) {
              unifiedSymbol += " (" + this.getOriginId(origin) + ")";
            }
          }
          record.add(unifiedSymbol);
          continue;
        case ORIGIN:
          // Origin
          record.add(origin);
          continue;
        case COLORS:
          /**
           * Color the diff columns depending on the origin
           */
          String highlight = IntStream.range(1, this.diffColumns.size())
            .mapToObj(i -> i + getColorByOrigin(origin))
            .collect(Collectors.joining(PrinterColor.COLOR_SEPARATOR));
          highlight += PrinterColor.COLOR_SEPARATOR;
          switch (diffStatus) {
            case DELETE:
              highlight = PrinterColor.RED.getLetterColor();
              break;
            case ADD:
              highlight = PrinterColor.GREEN.getLetterColor();
              break;
            case VALUE:
              highlight += cells
                .stream()
                .filter(cell -> cell.getEqualityStatus() != DataDiffEqualityStatus.STRICT_EQUAL)
                .map(cell -> {
                  int columnPositionWithDiffColumnAdded = cell.getColumnDef().getColumnPosition() + this.diffColumns.size();
                  String cellHighlight = String.valueOf(columnPositionWithDiffColumnAdded);
                  if (cell.getEqualityStatus() == DataDiffEqualityStatus.LOSS_EQUAL) {
                    return cellHighlight + PrinterColor.BLUE.getLetterColor();
                  }
                  return cellHighlight + getColorByOrigin(origin);
                })
                .collect(Collectors.joining(PrinterColor.COLOR_SEPARATOR));
              break;
            case NO_CHANGE:
              // no colors
              highlight = "";
            default:
              break;
          }
          record.add(highlight);
          continue;
        case ORIGIN_ID:
          record.add(this.getOriginId(origin));
          break;
        default:
          throw new InternalException("Comp Column (" + dataDiffColumn + ") should have been implemented");
      }


    }


    /**
     * Cell by column position
     * Cells may be null if there is no record to compare on both side
     * ie
     * * more record in the source than in the target
     * * or more record  in the target than in the source
     */
    for (DataDiffCell dataDiffCell : cells) {
      Object object = this.getCellValue(dataDiffCell, origin, diffStatus);
      record.add(object);
    }
    return record;
  }

  private Object getCellValue(DataDiffCell dataDiffCell, ValueOrigin origin, DataPathDiffStatus diffStatus) {

    /**
     * Sparse
     * On driver column equality, we put the value to null
     * if they are equals
     */
    if (!dataDiffCell.isDriverCell() && diffStatus == VALUE && diffBuilderMeta.reportDensity == DataDiffReportDensity.SPARSE && dataDiffCell.isEquals()) {
      return null;
    }
    /**
     * Dense
     */
    switch (origin) {
      case BOTH:
      case SOURCE:
        return dataDiffCell.getSourceValue();
      case TARGET:
        return dataDiffCell.getTargetValue();
      default:
        throw new InternalException("Type of origin (" + origin + ") is unknown");
    }


  }

  /**
   * Return the color by origin
   */
  private static String getColorByOrigin(ValueOrigin origin) {
    switch (origin) {
      case TARGET:
        /**
         * Added
         */
        return PrinterColor.GREEN.getLetterColor();
      case SOURCE:
        /**
         * Deleted
         */
        return PrinterColor.RED.getLetterColor();
      case BOTH:
      default:
        return "";
    }
  }

  private Object getOriginId(ValueOrigin origin) {
    switch (origin) {
      case BOTH:
      case SOURCE:
        return diffResult.getSourceRecordId();
      case TARGET:
        return diffResult.getTargetRecordId();
      default:
        throw new InternalException("Type of origin (" + origin + ") is unknown");
    }
  }


  @Override
  public DataPath getDataPath() {
    return this.accumulatorResultInsertStream.getDataPath();
  }

}
