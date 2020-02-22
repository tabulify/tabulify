package net.bytle.db.stream;

import net.bytle.db.engine.ThreadListener;
import net.bytle.db.engine.ThreadListenerAbs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gerard on 12-01-2017.
 * An object listener to of informations from the select stream
 * <p>
 * Example:
 * - exception and errors
 * - number of rows
 * - ...
 */
public class SelectStreamListener extends ThreadListenerAbs implements ThreadListener {

    private SelectStream selectStream;


    private List<RuntimeException> exceptions = new ArrayList<>();

    private SelectStreamListener(SelectStream selectStream) {

        this.selectStream = selectStream;

    }


    public static SelectStreamListener of(SelectStream selectStream) {

        return new SelectStreamListener(selectStream);

    }


    /**
     * The exit status:
     * - 0 if no errors occurs
     * - n: the number of exceptions otherwise
     *
     * @return
     */
    public int getExitStatus() {

        return exceptions.size();

    }


    public Long getRowCount() {
        return selectStream.getRow();
    }

    public SelectStream getSelectStream() {
        return selectStream;
    }

}
