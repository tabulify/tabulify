package net.bytle.log;


import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * An handler that send the output to System.out
 * Using a streamHandler in place of this handler suppress some message (strange)
 */
public class LogHandler extends StreamHandler {


    public LogHandler() {

        this.setOutputStream(System.out);

    }

    /**
     * Publish a <tt>LogRecord</tt>.
     * <p>
     * The logging request was made initially to a <tt>Logger</tt> object,
     * which initialized the <tt>LogRecord</tt> and forwarded it here.
     * <p>
     *
     * @param record description of the log event. A null record is
     *               silently ignored and is not published
     */
    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    /**
     * Override <tt>StreamHandler.close</tt> to do a flush but not
     * to close the output stream.  That is, we do <b>not</b>
     * close <tt>System.err</tt>.
     */
    @Override
    public void close() {
        flush();
    }

}

