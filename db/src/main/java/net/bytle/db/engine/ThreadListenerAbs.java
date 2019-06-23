package net.bytle.db.engine;

import java.util.ArrayList;
import java.util.List;

public class ThreadListenerAbs implements ThreadListener {


    private List<RuntimeException> exceptions = new ArrayList<>();

    @Override
    public List<RuntimeException> getExceptions() {
        return this.exceptions;
    }

    @Override
    public void addException(Exception e) {
        RuntimeException run;
        if (e.getClass().equals(RuntimeException.class)) {
            run = (RuntimeException) e;
        } else {
            run = new RuntimeException(e);
        }
        this.exceptions.add(run);
    }

}
