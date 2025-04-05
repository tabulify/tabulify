package net.bytle.email.flow.flow;

import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.spi.ConnectionProvider;
import net.bytle.type.Variable;

public class SmtpConnectionProvider extends ConnectionProvider {

  public static final String SMTP_SCHEME = "smtp";


  @Override
  public Connection createConnection(Tabular tabular, Variable name, Variable uri) {
    return new SmtpConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Variable uri) {
    return uri.getValueOrDefaultAsStringNotNull().startsWith(SMTP_SCHEME);
  }

}
