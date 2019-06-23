package net.bytle.db.resultSetDiff;



import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class ExecuteQueryThread implements Runnable, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ExecuteQueryThread.class.getPackage().toString());

    private final Statement statement;
    private final String query;
    private volatile ResultSet resultSet; // The Java volatile keyword guarantees visibility of changes to variables across threads.
    private volatile SQLException exception;
    private String connectionName;

    public ExecuteQueryThread(String connectionName, Statement statement, String query) {
        this.statement = statement;
        this.query = query;
        this.connectionName = connectionName;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            LOGGER.info("Executing the query against the connection " + connectionName);
            this.resultSet = statement.executeQuery(query);
            LOGGER.info("The query has returned from the connection " + connectionName);
        } catch (SQLException e) {
            LOGGER.severe(e.getMessage());
            this.exception = e;
        }

    }


    public ResultSet getResultSet() {
        return resultSet;
    }

    public Exception getError() {
        return exception;
    }

    public Boolean isError() {
        if (exception != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
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
     * <p>
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
        if (this.resultSet != null) {
            this.resultSet.close();
        }
    }
}
