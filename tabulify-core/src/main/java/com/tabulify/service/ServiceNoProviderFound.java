package com.tabulify.service;

/**
 * No provider found for system connection
 */
public class ServiceNoProviderFound extends RuntimeException {
  public ServiceNoProviderFound(String message) {
    super(message);
  }
}
