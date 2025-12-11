package com.tabulify.niofs.http;

import com.tabulify.type.KeyInterface;

public enum HttpHeader implements KeyInterface {

  /**
   * For now, we use CURL because some website returns 403
   * if they don't recognize it
   * Example:
   * MySQL: <a href="https://downloads.mysql.com/docs/world-db.zip">...</a>
   * Wikipedia: <a href="https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=xml&prop=description|categories">...</a>
   */
  USER_AGENT("curl/8.13.0"),
  AUTHORIZATION(null),
  ;


  private final String defaultValue;

  HttpHeader(String defaultValue) {

    this.defaultValue = defaultValue;

  }

  public String getDefaultValue() {
    return defaultValue;
  }

}
