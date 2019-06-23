package net.bytle.db.model;

import java.sql.ResultSet;

/**
 * No setter because of chaining initialization please
 */
public interface ISqlRelation extends RelationDef {


    String getQuery();

    ResultSet getResultSet();

}
