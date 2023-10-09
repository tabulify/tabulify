package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDefDefault;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.exception.NoColumnException;

import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

public class GenRelationDef extends RelationDefDefault {


  public static final int DEFAULT_DATA_TYPE = Types.VARCHAR;

  public <T extends DataPath> GenRelationDef(T DataPath) {
    super(DataPath);
  }


  public GenRelationDef addColumn(String columnName) {
    this.addColumn(columnName, Types.VARCHAR, null, null, null, null);
    return this;
  }

  @Override
  public GenRelationDef addColumn(String columnName, Integer typeCode) {
    this.addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public GenRelationDef addColumn(String columnName, int typeCode) {
    this.addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public GenRelationDef addColumn(String columnName, Integer type, Integer precision) {
    this.addColumn(columnName, type, precision, null, null, null);
    return this;
  }

  public GenRelationDef addColumn(String columnName, Integer type, Boolean nullable) {
    this.addColumn(columnName, type, null, null, nullable, null);
    return this;
  }

  public GenRelationDef addColumn(String columnName, Integer type, Integer precision, Integer scale) {
    this.addColumn(columnName, type, precision, scale, null, null);
    return this;
  }

  public GenRelationDef addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable) {
    this.addColumn(columnName, type, precision, scale, nullable, null);
    return this;
  }


  public GenRelationDef addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable, String comment) {

    if (type == null) {
      type = DEFAULT_DATA_TYPE;
    }

    SqlDataType dataType = this.getDataPath().getConnection().getSqlDataType(type);
    if (this.hasColumn(columnName)) {
      GenLog.LOGGER.warning("The column (" + columnName + ") was already defined, you can't add it");
      return this;
    }

    createColumn(columnName, dataType, dataType.getSqlClass())
      .precision(precision)
      .scale(scale)
      .setNullable(nullable)
      .setComment(comment);

    return this;

  }

  @Override
  public GenColumnDef createColumn(String columnName, SqlDataType sqlDataType, Class<?> clazz) {


    // This assert is to catch when object are passed
    // to string function, the length is bigger than the assertion and make it fails
    assert columnName.length() < 100;
    GenColumnDef columnDef = GenColumnDef.createOf(this, columnName, sqlDataType, clazz);
    ColumnDef oldColumn = columnDefByName.get(columnName);
    if (oldColumn == null) {
      /**
       * It's important to use the function {@link #getColumnsSize()}
       * and not {@link columnDefByName}
       * to get the size because columns may be hidden
       * {@link GenColumnDef#setIsHidden(boolean)}
       * and therefore does not count
       */
      int size = getColumnsSize();
      columnDef.setColumnPosition(size + 1);
    } else {
      columnDef.setColumnPosition(oldColumn.getColumnPosition());
    }
    super.columnDefByName.put(columnName, columnDef);

    return columnDef;

  }

  @Override
  public List<GenColumnDef> getColumnDefs() {
    return super.getColumnDefs()
      .stream()
      .map(e -> (GenColumnDef) e)
      .filter(GenColumnDef::isNotHidden)
      .sorted()
      .collect(Collectors.toList());
  }


  public GenRelationDef addColumn(String columnName, Integer type, Integer precision, Boolean nullable) {
    this.addColumn(columnName, type, precision, null, nullable, null);
    return this;
  }


  /**
   * Utility function to reset the data def
   * TODO: we may use the {@link DataPath#createRelationDef()} instead
   *
   * @return the relation
   */
  @Override
  public GenRelationDef dropAll() {
    super.dropAll();
    return this;
  }

  @Override
  public GenDataPath getDataPath() {

    return (GenDataPath) super.getDataPath();

  }

  @Override
  public GenColumnDef getColumnDef(String columnName) throws NoColumnException {
    return (GenColumnDef) super.getColumnDef(columnName);
  }

  @Override
  public GenColumnDef getOrCreateColumn(String columnName, Class<?> clazz) {

    return (GenColumnDef) super.getOrCreateColumn(columnName, clazz);

  }

  @Override
  public GenColumnDef getColumnDef(Integer columnIndex) {
    return (GenColumnDef) super.getColumnDef(columnIndex);
  }

  @Override
  public GenColumnDef getOrCreateColumn(String columnName, SqlDataType sqlDataType, Class<?> clazz) {

    if (this.hasColumn(columnName)) {
      throw new RuntimeException("The column (" + columnName + ") is already defined");
    }

    return createColumn(columnName, sqlDataType, clazz);

  }


  /**
   * We do this for the {@link GenColumnDef#setIsHidden(boolean) hidden column feature }
   */
  @Override
  public int getColumnsSize() {
    return getColumnDefs().size();
  }

  /**
   * We do this for the {@link GenColumnDef#setIsHidden(boolean) hidden column feature }
   */
  public List<GenColumnDef> getAllColumnDefs() {

    //noinspection unchecked
    return (List<GenColumnDef>) super.getColumnDefs();

  }

  @Override
  public GenRelationDef copyDataDef(DataPath fromDataPath) {
    return (GenRelationDef) super.copyDataDef(fromDataPath);
  }

  @Override
  public GenColumnDef getOrCreateColumn(String columnName) {
    return (GenColumnDef) super.getOrCreateColumn(columnName);
  }

}
