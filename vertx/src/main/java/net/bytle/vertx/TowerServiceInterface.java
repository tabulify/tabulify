package net.bytle.vertx;

import io.vertx.core.Future;

/**
 * A service is a class that the server mount, start and close
 * The class should add itself to the lists of
 * service in the {@link Server#registerService(TowerService)}
 *
 * The services have 4 points of management
 * * Their construction (mostly read configuration)
 * * Mounting (operations that are needed so that the server may answer)
 * * Start (operations that are needed just after. Example: health check, job, ...)
 * * Close (release resource and save runtime data)
 */
public interface TowerServiceInterface {


  /**
   * Before the server start
   * Create structure, needed to answer a request.
   * Example: OpenApi will read the file and create the router
   */
  Future<Void> mount() throws Exception;

  /**
   * Not all operations needs to be done on server mounting.
   * Some operations can be done just after.
   * Call just after the server has started
   * (for background service, build a queue, start scheduler)
   */
  Future<Void> start() throws Exception;

  /**
   * When the server stops
   */
  void close() throws Exception;

}
