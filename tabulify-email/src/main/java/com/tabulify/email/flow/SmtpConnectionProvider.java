package com.tabulify.email.flow;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.spi.ConnectionProvider;
import com.tabulify.conf.Attribute;

public class SmtpConnectionProvider extends ConnectionProvider {

  public static final String SMTP_SCHEME = "smtp";


  @Override
  public Connection createConnection(Tabular tabular, Attribute name, Attribute uri) {
    return new SmtpConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().startsWith(SMTP_SCHEME);
  }

}
