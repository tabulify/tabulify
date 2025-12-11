package com.tabulify.crypto;

import java.security.SecureRandom;

/**
 * Generate a random secret
 * It can be used to generate temporary secret
 * such as a CSRF token that can be regenerated for each
 * web server start.
 */
public class RandomSecret {

  @SuppressWarnings("SpellCheckingInspection")
  static final String DEFAULT_ALPHABET = "abcdefghijklmnopqrestuvwxyz0123456789-_?!@#$%*";

  private Integer secretSize;
  private int alphabetSize;
  private String alphabet;

  private final SecureRandom secureRand = new SecureRandom();

  public static Config config() {
    return new Config();
  }

  public String generate() {

    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < secretSize; i++) {
      /**
       * Example with Math.random
       * int indexValue = (int) (Math.random() * (alphabetSize + 0.99));
       */
      int indexValue = secureRand.nextInt(alphabetSize);
      stringBuilder.append(alphabet.charAt(indexValue));
    }


    return stringBuilder.toString();

  }

  public static class Config {
    private Integer size;
    private String alphabet;

    public Config setSize(int size) {
      this.size = size;
      return this;
    }

    @SuppressWarnings("unused")
    public Config setAlphabet(String alphabet) {
      this.alphabet = alphabet;
      return this;
    }

    public RandomSecret build() {
      RandomSecret randomSecret = new RandomSecret();
      if (size == null) {
        size = 10;
      }
      if (alphabet == null) {
        alphabet = DEFAULT_ALPHABET;
      }
      randomSecret.secretSize = size;
      randomSecret.alphabetSize = alphabet.length();
      randomSecret.alphabet = alphabet;
      return randomSecret;
    }
  }
}
