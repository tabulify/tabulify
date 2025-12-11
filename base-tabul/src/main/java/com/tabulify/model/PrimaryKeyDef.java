package com.tabulify.model;

import com.tabulify.exception.NoColumnException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PrimaryKeyDef implements Constraint {

  private final RelationDefAbs relationDef;
  private String name;

  /**
   * The position of the columns
   * are the position in the primary ket
   */
  private final List<ColumnDef<?>> columns = new ArrayList<>();

  public static PrimaryKeyDef of(RelationDefAbs relationDef, String... columnNames) {
    assert relationDef != null;
    assert columnNames.length > 0;
    assert columnNames[0] != null : "A column name must not be null";

    return new PrimaryKeyDef(relationDef, columnNames);
  }

  public String getName() {
    return name;
  }

  /**
   * Use {@link #PrimaryKeyDef(RelationDefAbs, String...)}
   */
  private PrimaryKeyDef() {
    throw new RuntimeException("Don't use this");
  }

  private PrimaryKeyDef(RelationDefAbs relationDef, String... columns) {
    this.relationDef = relationDef;
    Arrays.stream(columns).forEach(cn -> {
      ColumnDef columnDef;
      try {
        columnDef = relationDef.getColumnDef(cn);
        this.columns.add(columnDef);
      } catch (NoColumnException e) {
        throw new IllegalArgumentException("We could not create the primary of the resource (" + relationDef.getDataPath() + ") because the primary column name specified (" + cn + ") is not one of its columns. This resource knows only the following columns: (" + relationDef.getColumnDefs().stream().map(Objects::toString).collect(Collectors.joining(",")) + ')');
      }
    });
  }

  public RelationDef getRelationDef() {
    return relationDef;
  }

  @Override
  public ConstraintType getConstraintType() {
    return ConstraintType.PRIMARY_KEY;
  }

  public PrimaryKeyDef setName(String name) {
    this.name = name;
    return this;
  }


  public List<ColumnDef<?>> getColumns() {
    return columns;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PrimaryKeyDef that = (PrimaryKeyDef) o;
    return relationDef.equals(that.relationDef) &&
      columns.equals(that.columns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relationDef, columns);
  }

  @Override
  public String toString() {
    return "PrimaryKey of " + relationDef.getDataPath() + " (" + columns.stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")) + ")";
  }
}
