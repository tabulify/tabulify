/*
 * Copyright (c) 2014. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package net.bytle.db.fs.sql;


/**
 * Internal token representation.
 * <p/>
 * It is used as contract between the lexer and the parser.
 *
 */
public final class SqlPlusToken {

    /** length of the initial token (content-)buffer */
    private static final int INITIAL_TOKEN_LENGTH = 50;

    public enum Category {

        /** Sqlplus statement */
        SQLPLUS_STATEMENT,

        /** Token with Sql Statement */
        SQL_STATEMENT,

        /** Token with PlSql Statement */
        PLSQL_STATEMENT,

        /** COMMENT Statement */
        COMMENT,

    }

    /** Token type */
    Category category = null;

    /** The content buffer. */
    final StringBuilder content = new StringBuilder(INITIAL_TOKEN_LENGTH);


    void reset() {
        content.setLength(0);
        category = null;
    }

    /**
     * Eases IDE debugging.
     *
     * @return a string helpful for debugging.
     */
    @Override
    public String toString() {

        if (category != null) {
            return category.name()+ " [" + content.toString() + "]";
        } else {
            return "Token Unknown";
        }

    }

    public String getContent(){
        return content.toString();
    }

    public Category getCategory() {
        return category;
    }
}
