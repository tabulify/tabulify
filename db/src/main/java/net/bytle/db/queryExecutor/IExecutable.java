package net.bytle.db.queryExecutor;

import java.util.Properties;

public interface IExecutable extends Runnable {
    public IExecutable setup(String threadName, Properties properties) throws Exception;

    public IStatistics getStatistics();
}
