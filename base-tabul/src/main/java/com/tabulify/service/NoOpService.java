package com.tabulify.service;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;

/**
 * A noop service
 */
public class NoOpService extends Service {

  public NoOpService(Tabular tabular, Attribute name) {
    super(tabular, name);
  }

  @Override
  public void start() {
    throw new UnsupportedOperationException("The service " + this + " has the type (" + getType() + ") and the provider was not found. It can't be started. Install the (" + getType() + ") service provider or check your service type for a typo");
  }

  @Override
  public void drop() {
    throw new UnsupportedOperationException("The service " + this + " has the type (" + getType() + ") and the provider was not found. An instance can't be dropped. Install the (" + getType() + ") service provider or check your service type for a typo");
  }

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public void stop() {
    throw new UnsupportedOperationException("The service " + this + " has the type (" + getType() + ") and the provider was not found. It can't be stopped. Install the (" + getType() + ") service provider or check your service type for a typo.");
  }

  @Override
  public boolean exists() {
    throw new UnsupportedOperationException("The service " + this + " has the type (" + getType() + ") and the provider was not found. It can't be stopped. Install the (" + getType() + ") service provider or check your service type for a typo.");
  }

}
