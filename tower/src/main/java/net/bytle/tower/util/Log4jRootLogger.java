package net.bytle.tower.util;

import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.vertx.MailSmtpInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.SmtpAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * The root logger
 */
public class Log4jRootLogger {

  public static void configure(Log4JXmlConfiguration log4JXmlConfiguration) {

    LoggerConfig rootLogger = log4JXmlConfiguration.getRootLogger();

    /**
     * The console is the log for the services on Linux
     * If we want to see what's going we look on at the log of the services
     */
    rootLogger.addAppender(log4JXmlConfiguration.getConsoleAppender(), Level.WARN, null);

    /**
     * We catch the error that are not catche by
     * {@link ContextFailureHandler}
     */
    PatternLayout defaultLayout = PatternLayout.createDefaultLayout();
    FileAppender fileAppender = FileAppender.newBuilder()
      .setName("RootLogAppender")
      .setConfiguration(log4JXmlConfiguration)
      .withFileName("logs/failure-general.log")
      .setLayout(defaultLayout)
      .setImmediateFlush(true)
      .build();
    rootLogger.addAppender(fileAppender, Level.ERROR, null);


  }

  public static void configureOnAppInit(Configuration config, MailSmtpInfo mailSmtpInfo) {

    SmtpAppender smtpAppender = SmtpAppender.newBuilder()
      .setName("RootSmtpAppender")
      .setSmtpHost(mailSmtpInfo.getHost())
      .setConfiguration(config)
      .setSmtpPassword(mailSmtpInfo.getPassword())
      .setSmtpUsername(mailSmtpInfo.getUserName())
      .setSmtpPort(mailSmtpInfo.getPort())
      .setSubject("Log4j: General Failure in the tower app")
      .setFrom(UsersUtil.getEmailAddressWithName(SysAdmin.SYS_USER))
      .setTo(UsersUtil.getEmailAddressWithName(SysAdmin.ADMIN_USER))
      .build();
    config.addAppender(smtpAppender);

    config.getRootLogger()
      .addAppender(smtpAppender, Level.ERROR, null);
  }
}
