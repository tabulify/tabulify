package net.bytle.email.flow.flow;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import net.bytle.db.ProjectConfigurationFile;
import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.flow.Granularity;
import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.flow.step.SourceTargetHelperFunction;
import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectException;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.uri.DataUri;
import net.bytle.db.uri.DataUriString;
import net.bytle.email.BMailAddressStatic;
import net.bytle.email.BMailMimeMessage;
import net.bytle.email.BMailSmtpClient;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.fs.Fs;
import net.bytle.type.*;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * With a relational model, example:
 * <p>
 * <a href="https://cdn.cdata.com/help/DLE/jdbc/pg_table-mailmessages.htm">...</a>
 * INSERT INTO MailMessages (Subject, MessageBody, To) VALUES ('Test Subject','Body Text','address@company.com')
 * <p>
 * They also offer to send an email via a stored procedure
 * <a href="https://cdn.cdata.com/help/DLE/jdbc/pg_sp-sendmailmessage.htm">...</a>
 * ie a exec script
 * `
 * EXEC SendMailMessage To = 'test@test.com'
 * `
 */
public class SendmailStep extends FilterStepAbs {

  public static final String STEP_NAME = "sendmail";
  public static final String LOG_COLUMN_LOGICAL_NAME = "logical_name";
  public static final String LOG_COLUMN_TO = "to";
  public static final String LOG_COLUMN_SUBJECT = "subject";
  public static final String LOG_COLUMN_TIME = "time";
  public static final String LOG_COLUMN_MIME_MESSAGE = "mime_message";
  public static final String LOG_COLUMN_SMTP_SERVER = "smtp_server";


  RelationDef expectedLogDataPathStructure;

  private SmtpConnection connection;
  private List<InternetAddress> to;
  private InternetAddress from;
  private String subject;
  private String txt;
  private String html;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private List<MediaType> bodyMediaTypes = Arrays.asList(
    MediaTypes.TEXT_PLAIN,
    MediaTypes.TEXT_HTML
  );
  private Granularity granularity = Granularity.RESOURCE;
  private DataPath logDataPath = null;
  private List<InternetAddress> cc = null;
  private List<InternetAddress> bcc = null;


  public static SendmailStep create() {
    return new SendmailStep();
  }


  @Override
  public FilterRunnable createRunnable() {

    return new FilterRunnable() {


      private boolean isDone = false;
      private Set<DataPath> inputs;


      @Override
      public void addInput(Set<DataPath> inputs) {
        this.inputs = inputs;
      }

      @Override
      public void run() {

        switch (SendmailStep.this.granularity) {
          case RECORD:
            this.recordRun();
            break;
          case RESOURCE:
            this.resourceRun();
            break;
          default:
            throw new RuntimeException("Internal error: the granularity (" + SendmailStep.this.granularity + ") is not in the case statement and was not processed");
        }

        isDone = true;
      }

      private void resourceRun() {

        BMailMimeMessage.builder mimeMessage = BMailMimeMessage.createFromBuilder();
        SmtpConnection connectionRun = this.getConnectionRun();

        /**
         * From
         */
        from = SendmailStep.this.getFrom();
        if (from == null) {
          // never null
          from = connectionRun.getDefaultFromInternetAddress();
        }
        mimeMessage.setFromInternetAddress(from);
        /**
         * To
         */
        List<InternetAddress> to = SendmailStep.this.getTo();
        if (to == null) {
          to = connectionRun.getDefaultToInternetAddresses();
          if (to == null) {
            throw new RuntimeException("Error in this resource run, we couldn't determine the `to` email address (not defined in the step, nor as connection default). If you want to run a record run, you need to define it in `granularity`.");
          }
        }
        mimeMessage.addToInternetAddresses(to);
        /**
         * cc
         */
        List<InternetAddress> cc = SendmailStep.this.getCc();
        if (cc == null) {
          cc = connectionRun.getDefaultCcInternetAddresses();
        }
        if (cc != null) {
          mimeMessage.addCcInternetAddresses(cc);
        }
        /**
         * bcc
         */
        List<InternetAddress> bcc = SendmailStep.this.getBcc();
        if (bcc == null) {
          bcc = connectionRun.getDefaultBccInternetAddresses();
        }
        if (bcc != null) {
          mimeMessage.addBccInternetAddresses(bcc);
        }

        String subject = SendmailStep.this.getSubject();
        String messageLogicalName = inputs.stream().map(DataPath::getLogicalName).collect(Collectors.joining(", "));
        if (subject == null) {
          subject = messageLogicalName;
        }
        mimeMessage.setSubject(subject);
        String bodyTxt = SendmailStep.this.getTxt();
        if (bodyTxt != null) {
          mimeMessage.setBodyPlainText(bodyTxt);
        }
        String bodyHtml = SendmailStep.this.getHtml();
        if (bodyHtml != null) {
          mimeMessage.setBodyHtml(bodyHtml);
        }
        for (DataPath dataPath : inputs) {
          FsDataPath fsDataPath;
          if (dataPath instanceof FsDataPath) {
            fsDataPath = (FsDataPath) dataPath;
          } else {
            // tabular data from sql database or elsewhere
            // we download it, wow effect
            Path tempDirectory = Fs.createTempDirectory(SendmailStep.STEP_NAME);
            FsConnection tempConnection = tabular.createRuntimeConnectionFromLocalPath(tempDirectory.getFileName().toString(), tempDirectory);
            fsDataPath = SourceTargetHelperFunction.getTargetDataPathForFileSystem(dataPath, tempConnection);
            TransferManager transferManager = TransferManager.create().addTransfer(dataPath, fsDataPath).run();
            int exitStatus = transferManager
              .getExitStatus();
            if (exitStatus != 0) {
              throw new RuntimeException("Error while transferring the non-local resource " + dataPath + " to a temporary directory to add it to the email. Error:" + transferManager.getError());
            }
          }
          Path nioPath = fsDataPath.getNioPath();
          try {
            mimeMessage.addAttachment(nioPath);
          } catch (MessagingException e) {
            throw new RuntimeException("Error while adding the resource (" + dataPath + ") as attachment with the path (" + nioPath + "). Error:" + e.getMessage());
          }
        }

        BMailSmtpClient smtpServer = connectionRun.getSmtpServer();

        DataPath logDataPath = SendmailStep.this.getLogDataPathOrDefault();

        try (InsertStream logInsertStream = logDataPath.getInsertStream()) {
          try {
            this.sendMail(messageLogicalName, mimeMessage.build(), logInsertStream, smtpServer);
          } catch (MessagingException e) {
            throw new RuntimeException("Error while sending. Error: " + e.getMessage() +
              "\rSmtpServer:" + smtpServer +
              "\rMimeMessage:" + mimeMessage);
          }
        }


      }


      private SmtpConnection getConnectionRun() {
        SmtpConnection connection = SendmailStep.this.getConnection();
        if (connection != null) {
          return connection;
        }
        return (SmtpConnection) tabular.getConnection(Tabular.SMTP_CONNECTION);
      }

      private void recordRun() {


        SmtpConnection connectionRun = this.getConnectionRun();
        for (DataPath dataPath : this.inputs) {

          DataPath logDataPath = getLogDataPathOrDefault();


          List<BMailMimeMessage> bMailMimeMessageList = new ArrayList<>();

          try (InsertStream logInsertStream = logDataPath.getInsertStream()) {


            int recordId = 0;
            try (SelectStream selectStream = dataPath.getSelectStream()) {
              while (selectStream.next()) {
                recordId++;
                BMailMimeMessage.builder mimeMessage = BMailMimeMessage.createFromBuilder();

                /**
                 * From
                 */
                String from = selectStream.getString(SendmailStepArgument.FROM.toColumnName());
                InternetAddress fromAddress;
                if (from == null) {
                  fromAddress = SendmailStep.this.getFrom();
                  if (fromAddress == null) {
                    // never null
                    fromAddress = connectionRun.getDefaultFromInternetAddress();
                  }
                } else {
                  String fromName = selectStream.getString(SendmailStepArgument.FROM_NAME.toColumnName());
                  try {
                    fromAddress = BMailAddressStatic.addressAndNameToInternetAddress(from, fromName);
                  } catch (AddressException | UnsupportedEncodingException e) {
                    throw new RuntimeException("Error with the `from` addresses were found. Error: " + e.getMessage());
                  }
                }
                mimeMessage.setFromInternetAddress(fromAddress);
                /**
                 * To
                 */
                String to = selectStream.getString(SendmailStepArgument.TO.toColumnName());
                List<InternetAddress> toAddress;
                if (to == null) {
                  toAddress = SendmailStep.this.getTo();
                  if (toAddress == null) {
                    toAddress = connectionRun.getDefaultToInternetAddresses();
                    if (toAddress == null) {
                      throw new RuntimeException("We couldn't determine the `to` email address(es) (no defined in the record, nor in the step, nor as connection default");
                    }
                  }
                } else {
                  String toName = selectStream.getString(SendmailStepArgument.TO_NAMES.toColumnName());
                  try {
                    toAddress = BMailAddressStatic.addressAndNamesToListInternetAddress(to, toName);
                  } catch (AddressException | UnsupportedEncodingException e) {
                    throw new RuntimeException("Error with the `to` addresses were found. Error: " + e.getMessage());
                  }
                }
                mimeMessage.addToInternetAddresses(toAddress);
                /**
                 * Cc
                 */
                String cc = selectStream.getString(SendmailStepArgument.CC.toColumnName());
                List<InternetAddress> ccAddresses;
                if (cc == null) {
                  ccAddresses = SendmailStep.this.getCc();
                } else {
                  String ccName = selectStream.getString(SendmailStepArgument.CC_NAMES.toColumnName());
                  try {
                    ccAddresses = BMailAddressStatic.addressAndNamesToListInternetAddress(cc, ccName);
                  } catch (AddressException | UnsupportedEncodingException e) {
                    throw new RuntimeException("Error with the `cc` addresses were found. Error: " + e.getMessage());
                  }
                }
                if (ccAddresses != null) {
                  mimeMessage.addCcInternetAddresses(ccAddresses);
                }
                /**
                 * Bcc
                 */
                String bcc = selectStream.getString(SendmailStepArgument.CC.toColumnName());
                List<InternetAddress> bccAddresses;
                if (bcc == null) {
                  bccAddresses = SendmailStep.this.getBcc();
                } else {
                  String bccName = selectStream.getString(SendmailStepArgument.BCC_NAMES.toColumnName());
                  try {
                    bccAddresses = BMailAddressStatic.addressAndNamesToListInternetAddress(bcc, bccName);
                  } catch (AddressException | UnsupportedEncodingException e) {
                    throw new RuntimeException("Error with the `bcc` addresses were found. Error: " + e.getMessage());
                  }
                }
                if (bccAddresses != null) {
                  mimeMessage.addBccInternetAddresses(bccAddresses);
                }
                String subject = selectStream.getString(SendmailStepArgument.SUBJECT.toColumnName());
                if (subject == null) {
                  subject = SendmailStep.this.getSubject();
                  if (subject == null) {
                    subject = dataPath.getLogicalName() + " " + recordId;
                  }
                }
                mimeMessage.setSubject(subject);
                String bodyTxt = selectStream.getString(SendmailStepArgument.TXT.toColumnName());
                if (bodyTxt != null) {
                  mimeMessage.setBodyPlainText(bodyTxt);
                }
                String bodyHtml = selectStream.getString(SendmailStepArgument.HTML.toColumnName());
                if (bodyHtml != null) {
                  mimeMessage.setBodyHtml(bodyHtml);
                }
                if (bodyHtml == null && bodyTxt == null) {
                  throw new RuntimeException("We couldn't found a txt or html column for the `body` email");
                }
                try {
                  bMailMimeMessageList.add(mimeMessage.build());
                } catch (MessagingException e) {
                  throw new RuntimeException("Error while building the message",e);
                }
              }
            } catch (SelectException e) {
              throw new RuntimeException("Error while getting the select stream for the data path (" + dataPath + ")");
            }

            BMailSmtpClient smtpServer = connectionRun.getSmtpServer();

            for (BMailMimeMessage bMailMimeMessage : bMailMimeMessageList) {
              try {
                this.sendMail(dataPath.getLogicalName(), bMailMimeMessage, logInsertStream, smtpServer);
              } catch (MessagingException e) {
                throw new RuntimeException("Error while sending. " +
                  "Error: " + e.getMessage() +
                  "\nSmtpServer: " + smtpServer)
                  ;
              }

            }

          }

        }

      }

      private void sendMail(String logicalName, BMailMimeMessage bMailMimeMessage, InsertStream logInsertStream, BMailSmtpClient smtpServer) throws MessagingException {

        String originalToMessageAddresses = bMailMimeMessage.getToAsAddresses().toString();
        String environment = tabular.getEnvironment();
        /**
         * Modify the `to` email
         */
        if (!environment.equals(ProjectConfigurationFile.PRODUCTION_ENV)) {

          List<InternetAddress> toMessagesAddresses = bMailMimeMessage.getToInternetAddresses();
          if (toMessagesAddresses.size() == 0) {
            // should not happen as it's checked before
            throw new RuntimeException("A `to` address was not found.");
          }

          SmtpConnection connectionRun = this.getConnectionRun();
          List<InternetAddress> connectionTos = connectionRun.getDefaultToInternetAddresses();
          if (connectionTos.size() == 0) {
            throw new RuntimeException("A `to` address was not specified for the connection (" + connectionRun + "). It's mandatory because the execution runs with the environment (" + environment + ") that is not a production environment, therefore the email are send to the `to` address of the connection. Set the environment to production or give a `to` email property to smtp.");
          }
          InternetAddress connectionTo = connectionTos.get(0);
          for (InternetAddress internetAddress : toMessagesAddresses) {
            String oldAddress = internetAddress.getAddress();
            internetAddress.setAddress(connectionTo.getAddress());
            try {
              String personal = internetAddress.getPersonal();
              if (personal == null) {
                personal = "";
              } else {
                personal = personal + " ";
              }
              personal = personal + "(not in " + ProjectConfigurationFile.PRODUCTION_ENV + " environment, receiver address (" + oldAddress + ") was changed to connection default)";
              internetAddress.setPersonal(personal);
            } catch (UnsupportedEncodingException e) {
              throw new RuntimeException("Error while changing the personal of the to email address before sending. Error:" + e.getMessage());
            }
          }
          bMailMimeMessage.setToInternetAddresses(toMessagesAddresses);
        }

        /**
         * Send
         */
        smtpServer.sendMessage(bMailMimeMessage);

        /**
         * Log
         */
        Date sentDate;
        try {
          sentDate = bMailMimeMessage.getSentDate();
        } catch (MessagingException e) {
          // not send
          sentDate = new Date();
        }
        logInsertStream.insert(
          sentDate,
          logicalName,
          originalToMessageAddresses,
          bMailMimeMessage.getSubject(),
          bMailMimeMessage.toEml(),
          smtpServer.toString()
        );
      }

      @Override
      public Set<DataPath> get() {
        return inputs;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return this.isDone;
      }

      @Override
      public Set<DataPath> get(long timeout, TimeUnit unit) {
        throw new RuntimeException("No queue here");
      }

    };
  }

  private List<InternetAddress> getBcc() {
    return this.bcc;
  }

  private List<InternetAddress> getCc() {
    return this.cc;
  }

  private DataPath getLogDataPathOrDefault() {
    if (logDataPath != null) {
      return logDataPath;
    }
    logDataPath = tabular.getLogsConnection().getDataPath("smtp");
    SendmailStep.this.checkAndOrCreateLogDataPath(logDataPath);
    return logDataPath;
  }

  private String getHtml() {
    return this.html;
  }

  private String getTxt() {
    return this.txt;
  }

  private String getSubject() {
    return subject;
  }

  private InternetAddress getFrom() {
    return this.from;
  }

  private List<InternetAddress> getTo() {
    return this.to;
  }

  @Override
  public boolean isAccumulator() {
    // we send all resources at once in one email
    return true;
  }

  private SmtpConnection getConnection() {
    return this.connection;
  }

  @Override
  public String getOperationName() {
    return STEP_NAME;
  }

  @Override
  public OperationStep setArguments(MapKeyIndependent<Object> arguments) {
    for (Map.Entry<String, Object> entry : arguments.entrySet()) {
      Object value = entry.getValue();
      if (value == null) {
        throw new IllegalStateException("The value of the property (" + entry.getKey() + ") of the step (" + this + ") is null.");
      }
      SendmailStepArgument emailStepArgument;
      try {
        emailStepArgument = Casts.cast(entry.getKey(), SendmailStepArgument.class);
      } catch (CastException e) {
        throw new IllegalStateException("The value of the property (" + entry.getKey() + ") of the step (" + this + ") is not a send mail argument. You can use on of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(SendmailStepArgument.class));
      }
      String stringValue = value.toString();
      switch (emailStepArgument) {
        case TARGET_URI:
          String atChar = String.valueOf(DataUriString.AT_CHAR);
          if (!stringValue.startsWith(atChar)) {
            throw new RuntimeException("The target uri value (" + stringValue + ") of the " + this + " step should be a connection uri and starts with the at character (" + atChar + ")");
          }
          DataUri dataUri = tabular.createDataUri(stringValue);
          Connection yamlConnection = dataUri.getConnection();
          if (!(yamlConnection instanceof SmtpConnection)) {
            throw new IllegalArgumentException("The email connection (" + yamlConnection + ") of the " + this + " step is not a smtp connection but a " + yamlConnection.getClass().getSimpleName());
          }
          this.setConnection((SmtpConnection) yamlConnection);
          break;
        case TO:
          if (value instanceof Map) {
            Map<String, String> tos;
            try {
              tos = Casts.castToSameMap(value, String.class, String.class);
            } catch (CastException e) {
              throw new InternalException("String, string should not result in a cast exception",e);
            }
            try {
              this.setTo(BMailAddressStatic.mapToListInternetAddress(tos));
            } catch (AddressException | UnsupportedEncodingException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.TO + " map email property (address:name). Error: " + e.getMessage());
            }
          } else if (value instanceof String) {
            try {
              this.setTo(BMailAddressStatic.stringToListInternetAddress(stringValue));
            } catch (AddressException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.TO + " email list (" + value + "). Error: " + e.getMessage());
            }
          }
          break;
        case FROM:
          if (value instanceof Map) {
            Map<String, String> froms;
            try {
              froms = Casts.castToSameMap(value, String.class, String.class);
            } catch (CastException e) {
              throw new InternalException("String, string should not result in a cast exception",e);
            }
            try {
              List<InternetAddress> internetAddresses = BMailAddressStatic.mapToListInternetAddress(froms);
              if (internetAddresses.size() > 1) {
                throw new RuntimeException("Only one `from` address can be specified. The map `from` value (" + value + ") has more than one");
              }
              this.setFrom(internetAddresses.get(0));
            } catch (AddressException | UnsupportedEncodingException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.FROM + " map email property (address:name). Error: " + e.getMessage());
            }
          } else if (value instanceof String) {
            try {
              List<InternetAddress> internetAddresses = BMailAddressStatic.stringToListInternetAddress(stringValue);
              if (internetAddresses.size() > 1) {
                throw new RuntimeException("Only one `from` address can be specified. The from value (" + value + ") has more than one");
              }
              this.setFrom(internetAddresses.get(0));
            } catch (AddressException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.FROM + " email list (" + value + "). Error: " + e.getMessage());
            }
          }
          break;
        case CC:
          if (value instanceof Map) {
            Map<String, String> tos;
            try {
              tos = Casts.castToSameMap(value, String.class, String.class);
            } catch (CastException e) {
              throw new InternalException("String, string should not result in a cast exception",e);
            }
            try {
              this.setCc(BMailAddressStatic.mapToListInternetAddress(tos));
            } catch (AddressException | UnsupportedEncodingException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.CC + " map email property (address:name). Error: " + e.getMessage());
            }
          } else if (value instanceof String) {
            try {
              this.setCc(BMailAddressStatic.stringToListInternetAddress(stringValue));
            } catch (AddressException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.CC + " email list (" + value + "). Error: " + e.getMessage());
            }
          }
          break;
        case BCC:
          if (value instanceof Map) {
            Map<String, String> tos;
            try {
              tos = Casts.castToSameMap(value, String.class, String.class);
            } catch (CastException e) {
              throw new InternalException("String, string should not result in a cast exception",e);
            }
            try {
              this.setBcc(BMailAddressStatic.mapToListInternetAddress(tos));
            } catch (AddressException | UnsupportedEncodingException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.BCC + " map email property (address:name). Error: " + e.getMessage());
            }
          } else if (value instanceof String) {
            try {
              this.setBcc(BMailAddressStatic.stringToListInternetAddress(stringValue));
            } catch (AddressException e) {
              throw new RuntimeException("Error while reading the " + SendmailStepArgument.BCC + " email list (" + value + "). Error: " + e.getMessage());
            }
          }
          break;
        case SUBJECT:
          this.setSubject(stringValue);
          break;
        case TXT:
          this.setBodyText(stringValue);
          break;
        case HTML:
          this.setBodyHtml(stringValue);
          break;
        case LOG_TARGET_URI:
          this.setLogDataPath(tabular.getDataPath(stringValue));
          break;
        case BODY_TYPE:
          List<MediaType> mediaTypes = new ArrayList<>();
          if (value instanceof String) {
            MediaType mediaType;
            try {
              mediaType = MediaTypes.createFromMediaTypeString(stringValue);
            } catch (NullValueException e) {
              throw new IllegalArgumentException("The value of the (" + SendmailStepArgument.BODY_TYPE + ") should not be null for the send mail step.");
            }
            mediaTypes.add(mediaType);
          } else {
            if (value instanceof List) {
              List<String> mediaTypeStrings = Casts.castToListSafe(value, String.class);
              for (String mediaTypeString : mediaTypeStrings) {
                try {
                  MediaType mediaType = MediaTypes.createFromMediaTypeString(mediaTypeString);
                  mediaTypes.add(mediaType);
                } catch (NullValueException e) {
                  throw new IllegalArgumentException("One the list value of the (" + SendmailStepArgument.BODY_TYPE + ") should not be null for the send mail step.");
                }
              }
            } else {
              throw new RuntimeException("The value of body type in the step (" + this + ") should be a string or a list but is a " + value.getClass().getSimpleName());
            }
          }
          this.setBodyMediaType(mediaTypes);
          break;
        case STEP_GRANULARITY:
          Granularity granularity;
          try {
            granularity = Casts.cast(stringValue, Granularity.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The granularity value (" + stringValue + ") is not a valid granularity for the step (" + this + "). You may enter one of the following granularity: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(Granularity.class));
          }
          this.setGranularity(granularity);
          break;
        default:
          throw new RuntimeException("Internal Error: the email argument (" + emailStepArgument + ") is not processed");
      }
    }
    if (this.connection == null) {
      Connection yamlConnection = tabular.getConnection(Tabular.SMTP_CONNECTION);
      if (yamlConnection == null) {
        throw new RuntimeException("The target uri argument (" + SendmailStepArgument.TARGET_URI + ") or the default email connection (" + Tabular.SMTP_CONNECTION + ") is mandatory for the " + this + " step but none was found");
      }
      if (!(yamlConnection instanceof SmtpConnection)) {
        throw new IllegalArgumentException("The default email connection (" + Tabular.SMTP_CONNECTION + ") of the " + this + " step is not a smtp connection but a " + yamlConnection.getClass().getSimpleName());
      }
      this.setConnection((SmtpConnection) yamlConnection);
    }
    return this;
  }

  private SendmailStep setBcc(List<InternetAddress> internetAddresses) {
    this.bcc = internetAddresses;
    return this;
  }

  private SendmailStep setCc(List<InternetAddress> internetAddresses) {
    this.cc = internetAddresses;
    return this;
  }


  private SendmailStep setLogDataPath(DataPath dataPath) {
    this.logDataPath = dataPath;
    this.checkAndOrCreateLogDataPath(dataPath);
    return this;
  }

  private void checkAndOrCreateLogDataPath(DataPath logDataPath) {

    /**
     * See also
     * the `columns` section of:
     * https://cdn.cdata.com/help/DLE/jdbc/pg_table-mailmessages.htm
     */
    expectedLogDataPathStructure = tabular.getAndCreateRandomMemoryDataPath().getOrCreateRelationDef()
      .addColumn(LOG_COLUMN_TIME, Types.TIMESTAMP)
      .addColumn(LOG_COLUMN_LOGICAL_NAME)
      .addColumn(LOG_COLUMN_TO)
      .addColumn(LOG_COLUMN_SUBJECT)
      .addColumn(LOG_COLUMN_MIME_MESSAGE, Types.CLOB)
      .addColumn(LOG_COLUMN_SMTP_SERVER);

    if (!Tabulars.exists(logDataPath)) {
      logDataPath.getOrCreateRelationDef().copyDataDef(expectedLogDataPathStructure.getDataPath());
      Tabulars.create(logDataPath);
    } else {
      for (ColumnDef columnDef : expectedLogDataPathStructure.getColumnDefs()) {
        if (!logDataPath.getOrCreateRelationDef().hasColumn(columnDef.getColumnName())) {
          throw new RuntimeException("The log column (" + columnDef.getColumnName() + ") does not exist in the existing email log data path (" + logDataPath + ")");
        }
      }
    }
  }

  public SendmailStep setGranularity(Granularity granularity) {
    this.granularity = granularity;
    return this;
  }

  private SendmailStep setBodyText(String text) {
    this.txt = text;
    return this;
  }

  private SendmailStep setBodyHtml(String html) {
    this.html = html;
    return this;
  }

  private SendmailStep setBodyMediaType(List<MediaType> html) {
    this.bodyMediaTypes = html;
    return this;
  }

  private SendmailStep setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  private SendmailStep setFrom(InternetAddress internetAddresse) {
    this.from = internetAddresse;
    return this;
  }

  private SendmailStep setTo(List<InternetAddress> internetAddresses) {
    this.to = internetAddresses;
    return this;
  }


  public OperationStep setConnection(SmtpConnection connection) {
    this.connection = connection;
    return this;
  }


}
