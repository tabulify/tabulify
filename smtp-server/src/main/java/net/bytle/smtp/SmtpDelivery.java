package net.bytle.smtp;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import net.bytle.vertx.ConfigAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class SmtpDelivery implements Handler<Long> {

  private static final String DELIVERY_RETRY_INTERVAL_KEY = "delivery.retry.interval";
  private static final Integer DEFAULT_INTERVAL = 1;
  private static final String DELIVERY_INTERVAL_KEY = "delivery.interval";
  static Logger LOGGER = LogManager.getLogger(SmtpDelivery.class);
  private static final int DEFAULT_RETRY_INTERVAL = 15;
  private final Map<Integer, SmtpDeliveryEnvelope> deliveryQueue = new HashMap<>();
  private final Integer retryIntervalBetweenFailures;
  private final Vertx vertx;
  private boolean isRunning = false;
  private Instant lastDeliveringTentative;

  public SmtpDelivery(Vertx vertx, ConfigAccessor configAccessor) {

    this.vertx = vertx;

    Integer deliveryInterval = configAccessor.getInteger(DELIVERY_INTERVAL_KEY, DEFAULT_INTERVAL);
    LOGGER.info("Delivery interval (" + DELIVERY_RETRY_INTERVAL_KEY + ") set to " + deliveryInterval + " minutes");
    vertx.setPeriodic(deliveryInterval * 60 * 1000, this);

    this.retryIntervalBetweenFailures = configAccessor.getInteger(DELIVERY_RETRY_INTERVAL_KEY, DEFAULT_RETRY_INTERVAL);
    LOGGER.info("Delivery retry interval (" + DELIVERY_RETRY_INTERVAL_KEY + ") between failures set to " + this.retryIntervalBetweenFailures + " minutes");

  }

  @Override
  public void handle(Long event) {

    this.run();

  }

  void run() {
    LOGGER.info("Delivery started");
    if (deliveryQueue.size() == 0) {
      LOGGER.info("Delivery: Nothing to deliver");
      return;
    }
    if (this.isRunning) {
      return;
    }

    this.vertx.executeBlocking(() -> {

      this.isRunning = true;
      List<Future<Void>> envelopeDeliveries = new ArrayList<>();

      for (Integer enveloppeId : deliveryQueue.keySet()) {
        SmtpDeliveryEnvelope envelope = deliveryQueue.get(enveloppeId);
        Future<Void> envelopeDelivery = this.deliver(envelope)
          .compose(v -> {
            if (envelope.hasBeenDelivered()) {
              deliveryQueue.remove(enveloppeId);
            }
            return Future.succeededFuture();
          });
        envelopeDeliveries.add(envelopeDelivery);
      }
      return Future.join(envelopeDeliveries)
        .compose(ar -> {
          this.isRunning = false;
          LOGGER.info("Delivery done");
          return Future.succeededFuture();
        });

    });
  }

  public void addEnvelopeToDeliver(SmtpDeliveryEnvelope enveloppe) {
    this.deliveryQueue.put(enveloppe.hashCode(), enveloppe);
  }

  /**
   * 2 choices:
   * * local delivery: storing the email in a local mailbox (disk, http endpoint)
   * * remote delivery: transmitting it to remote mailbox with or without forwarding (alias): SRS
   */
  public Future<Void> deliver(SmtpDeliveryEnvelope smtpDeliveryEnvelope) {

    if (this.lastDeliveringTentative != null) {
      long duration = Duration.between(this.lastDeliveringTentative, Instant.now()).toMinutes();
      if (duration < retryIntervalBetweenFailures) {
        return Future.succeededFuture();
      }
    }

    this.lastDeliveringTentative = Instant.now();

    /**
     * Recipients
     */
    Set<SmtpRecipient> recipientsPaths = smtpDeliveryEnvelope.getRecipientsToDeliver();
    List<Future<Void>> delivery = new ArrayList<>();
    for (SmtpRecipient recipient : recipientsPaths) {

      delivery.add(this.deliverToRecipient(recipient, smtpDeliveryEnvelope));

    }
    return Future.join(delivery)
      .compose(res -> Future.succeededFuture());


  }

  private Future<Void> deliverToRecipient(SmtpRecipient recipient, SmtpDeliveryEnvelope smtpDeliveryEnvelope) {
    Future<Void> deliveryFuture;
    SmtpDeliveryType deliveryType = recipient.getDeliveryType();
    switch (deliveryType) {
      case LOCAL:
        deliveryFuture = recipient.getLocalUser().deliver(smtpDeliveryEnvelope);
        break;
      case REMOTE:
        /**
         * Gmail's Bulk Senders Guidelines
         * https://support.google.com/mail/answer/81126
         * DMARC/Postmaster tool
         * https://support.google.com/mail/answer/2451690
         */
        deliveryFuture = Future.failedFuture("Remote delivery is not yet supported");
        break;
      default:
        deliveryFuture = Future.failedFuture("Internal Error: Delivery Type (" + deliveryType + ") is unknown");
    }
    return deliveryFuture.compose(ar -> {
        smtpDeliveryEnvelope.hasBeenDeliveredToRecipient(recipient);
        return Future.succeededFuture();
      }, err -> {
        smtpDeliveryEnvelope.deliveryFailureForRecipient(recipient, err);
        return Future.failedFuture(err);
      }
    );
  }


}
