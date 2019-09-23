package net.bytle.db.model;


import net.bytle.db.database.Database;
import net.bytle.db.engine.Queries;
import net.bytle.db.engine.ResultSets;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A class that contains a query structure definition
 */
public class QueryDef extends RelationDefAbs implements ISqlRelation, AutoCloseable {


    private final String query;
    private ResultSet resultSet;


    protected QueryDef(Database database, String query, String queryName) {

        super();

        if (queryName == null) {
            this.name = query;
        } else {
            this.name = queryName;
        }
        this.query = query;
        this.schema = database.getCurrentSchema();


    }

    protected QueryDef(Database database, String query) {
        this(database, query, null);
    }


    public QueryDef addColumn(String columnName) {

        if (meta == null) {
            initMeta();
        }
        meta.getColumnDef(columnName);
        return this;
    }

    private void initMeta() {

        meta = new RelationMeta(this);
        resultSet = getResultSet();
        ResultSets.addColumns(resultSet, this);

    }


    /**
     * Return the columns by position
     *
     * @return
     */
    public List<ColumnDef> getColumnDefs() {
        if (meta == null) {
            initMeta();
        }
        return meta.getColumnDefs();

    }






    /**
     * @param columnName
     * @return the column or null if not found
     */
    public ColumnDef getColumnDef(String columnName) {

        if (meta == null) {
            initMeta();
        }
        return meta.getColumnDef(columnName);

    }



    @Override
    public ColumnDef getColumnDef(Integer columnIndex) {

        if (meta == null) {
            initMeta();
        }
        return meta.getColumnDef(columnIndex);

    }

    /**
     * @param columnName
     * @param clazz      - The type of the column (Java needs the type to be a sort of type safe)
     * @return a new columnDef
     */
    @Override
    public ColumnDef getColumnOf(String columnName, Class clazz) {
        return meta.getColumnOf(columnName,clazz);
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public ResultSet getResultSet() {
        try {
            if (resultSet == null || resultSet.isClosed()) {
                resultSet = Queries.getResultSet(this);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return resultSet;
    }

    @Override
    public String toString() {
        return getName();
    }

    public QueryDef setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     *
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     *
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     *
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     *
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {
        resultSet.close();
    }
}
