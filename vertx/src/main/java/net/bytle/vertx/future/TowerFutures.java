package net.bytle.vertx.future;

import net.bytle.vertx.Server;

/**
 * A class that holds all custom Future Coordination
 * on list via Schedulers
 */
public class TowerFutures {

  private final Server server;

  public TowerFutures(Server server) {
    this.server = server;
  }

  /**
   * Execute future sequentially one by one
   * @return void when finished
   */
  @SuppressWarnings("unused") // tClass is used by the code analytics to define the type on the function level
  public <T> TowerFuturesSequentialScheduler<T> createSequentialScheduler(Class<T> tClass) {

    return new TowerFuturesSequentialScheduler<>();

  }

  /**
   * Execute futures in batch mode and with rate limiting
   */
  @SuppressWarnings("unused") // tClass is used by the code analytics to define the type on the function level
  public <T> TowerFuturesRateLimitedScheduler.Builder<T> createRateLimitedCoordinationScheduler(Class<T> tClass) {

    return new TowerFuturesRateLimitedScheduler.Builder<>(this.server);

  }

}
