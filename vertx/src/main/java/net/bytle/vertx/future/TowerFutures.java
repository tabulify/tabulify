package net.bytle.vertx.future;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
  public <T extends Handler<Promise<T>>> TowerFuturesSequentialScheduler.Config<T> createSequentialScheduler(Class<T> tClass) {

    return new TowerFuturesSequentialScheduler.Config<>();

  }

  /**
   * Execute futures in batch mode and with rate limiting
   */
  @SuppressWarnings("unused") // tClass is used by the code analytics to define the type on the function level
  public <T extends Handler<Promise<T>>> TowerFuturesRateLimitedScheduler.Builder<T> createRateLimitedCoordinationScheduler(Class<T> tClass) {

    return new TowerFuturesRateLimitedScheduler.Builder<>(this.server);

  }

}
