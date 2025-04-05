package net.bytle.db.model;

public interface Constraint {

  /**
   *
   * @return the name of the constraint
   * Used to drop a constraint by name
   *
   * The name is not mandatory when creating a constraint
   *    * via a create statement
   *    * but is mandatory to delete it
   */
  String getName();

  /**
   * The constraint is a child of a data def
   * @return
   */
  RelationDef getRelationDef();

}
