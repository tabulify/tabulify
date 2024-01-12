package net.bytle.vertx.future;

import io.vertx.core.Future;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This class will execute Futures in a rate limited fashion
 *
 * @param <T>
 */
public class TowerFuturesRateLimitedScheduler<T> {


  private final int batchSize;
  private final Duration rateDuration;
  private final int rateCount;
  private final Server server;


  public TowerFuturesRateLimitedScheduler(Builder<T> builder) {
    this.rateCount = builder.rateCount;
    int ratePeriodAmount = builder.ratePeriodeAmount;
    TimeUnit ratePeriodUnit = builder.ratePeriodeUnit;
    this.rateDuration = Duration.of(ratePeriodAmount, ratePeriodUnit.toChronoUnit());
    this.batchSize = builder.batchSize;
    this.server = builder.server;
  }

  public static <T> Builder<T> Builder(Server server) {
    return new Builder<>(server);
  }

  public Future<TowerFutureComposite<T>> all(Collection<Future<T>> futures, TowerCompositeFutureListener listener) {
    TowerFuturesRateLimitedComposite<T> towerComposite = new TowerFuturesRateLimitedComposite<>(this, futures, TowerFutureCoordination.ALL, listener);
    return Future.future(towerComposite);
  }


  TemporalAmount getRatePeriodeDuration() {
    return this.rateDuration;
  }

  int getBatchSize() {
    return this.batchSize;
  }

  int getRateCount() {
    return this.rateCount;
  }

  Server getServer() {
    return server;
  }


  public static class Builder<T> {
    private final Server server;
    private int rateCount = 150;
    private int batchSize = 5;

    private int ratePeriodeAmount = 5;
    private TimeUnit ratePeriodeUnit = TimeUnit.SECONDS;

    public Builder(Server server) {
      this.server = server;
    }

    /**
     * @param batchSize - the number of future executed at a time
     */
    public Builder<T> setBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    /**
     * @param amount - the maximum execution that should occur in a rate period of time
     * @param timeAmount    - the rate period amount
     * @param timeUnit  - the rate period unit
     */
    public Builder<T> setRateLimit(int amount, int timeAmount, TimeUnit timeUnit) {
      this.rateCount = amount;
      this.ratePeriodeAmount = timeAmount;
      this.ratePeriodeUnit = timeUnit;
      return this;
    }

    /**
     * Execute and fail at the first failure
     */
    public TowerFuturesRateLimitedScheduler<T> build() {
      return new TowerFuturesRateLimitedScheduler<>(this);
    }

  }
}
