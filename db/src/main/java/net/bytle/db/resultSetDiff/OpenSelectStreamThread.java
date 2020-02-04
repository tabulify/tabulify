package net.bytle.db.resultSetDiff;


import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.SelectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSelectStreamThread implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSelectStreamThread.class);


  private final DataPath dataPath;
  // private volatile ResultSet resultSet; // The Java volatile keyword guarantees visibility of changes to variables across threads.
  private SelectStream selectStream;
  private Exception exception;

  public OpenSelectStreamThread(DataPath dataPath) {

    this.dataPath = dataPath;

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
      LOGGER.info("Getting the read stream from the dataPath (" + dataPath + ")");
      this.selectStream = Tabulars.getSelectStream(dataPath);
      this.selectStream.execute();
      LOGGER.info("The read stream has returned from the data path (" + dataPath + ")");
    } catch (Exception e){
      exception = e;
    }

  }


  public SelectStream getSelectStream() {
    return selectStream;
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


}
