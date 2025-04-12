package com.tabulify.model;

import java.util.*;
import java.util.stream.Collectors;

public class UniqueKeyDef implements Constraint {

  private final RelationDef relationDef;
  private String name;
  private Map<Integer, ColumnDef> columnDefs = new HashMap<>();

  public static UniqueKeyDef of(RelationDef relationDef) {
    return new UniqueKeyDef(relationDef);
  }

  /**
   * The name is not mandatory when creating a constraint
   * via a create statement
   * but is mandatory to delete it
   *
   * @return
   */
  public String getName() {
    return name;
  }



  UniqueKeyDef(RelationDef relationDef) {
    this.relationDef = relationDef;
  }

  public RelationDef getRelationDef() {
    return relationDef;
  }

  public UniqueKeyDef name(String name) {
    this.name = name;
    return this;
  }

  public UniqueKeyDef addColumn(ColumnDef columnDef) {

    return addColumn(columnDef, this.columnDefs.keySet().size() + 1);
  }

  public UniqueKeyDef addColumn(ColumnDef columnDef, int colSeq) {

    if (!this.columnDefs.containsValue(columnDef)) {

      this.columnDefs.put(colSeq, columnDef);

    }

    return this;

  }

  /**
   * Return the columns sorted by position
   *
   * @return
   */
  public List<ColumnDef> getColumns() {

    List<Integer> positions = new ArrayList<>(columnDefs.keySet());
    Collections.sort(positions);
    List<ColumnDef> columnDefsToReturn = new ArrayList<>();
    for (Integer position : positions) {
      columnDefsToReturn.add(columnDefs.get(position));
    }
    return columnDefsToReturn;
  }


  public UniqueKeyDef addColumns(List<ColumnDef> columnDefs) {

    for (ColumnDef columnDef : columnDefs) {
      addColumn(columnDef);
    }

    return this;

  }

  @Override
  public String toString() {
    return "Unique Key of " + relationDef.getDataPath() + " (" + columnDefs.values().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(",")) + ')';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UniqueKeyDef that = (UniqueKeyDef) o;
    return relationDef.equals(that.relationDef) &&
      columnDefs.equals(that.columnDefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relationDef, columnDefs);
  }

}
