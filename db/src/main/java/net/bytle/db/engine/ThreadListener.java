package net.bytle.db.engine;

import java.util.List;

public interface ThreadListener {


    public List<RuntimeException> getExceptions();


    public void addException(Exception e);

}
