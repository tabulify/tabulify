package net.bytle.email;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import net.bytle.exception.NotAbsoluteException;
import net.bytle.exception.NullValueException;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypeExtension;
import net.bytle.type.MediaTypes;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A builder for the internet text messages (known as a mail)
 * A wrapper around a {@link MimeMessage}
 * See also the new {@link com.sun.mail.smtp.SMTPMessage}
 * that wraps the Mime Message for other options
 * <p>
 * @link <a href="https://www.rfc-editor.org/rfc/rfc5322">Last rfc on message format</a>
 * @link <a href="https://www.rfc-editor.org/rfc/rfc822.html">...</a>
 * @link <a href="https://docs.oracle.com/javaee/6/api/javax/mail/internet/MimeMessage.html">...</a>
 */
public class BMailMimeMessage {


  private static final Charset emailCharset = StandardCharsets.UTF_8;

  private final MimeMessage mimeMessage;

  /**
   * content parts is there to
   * be able to construct a multipart email
   * You can't do when setting html or text body
   * because this is not the same
   */
  private final List<MimeBodyPart> attachmentParts = new ArrayList<>();
  private final MediaType HTML_MIME = MediaTypes.TEXT_HTML;
  private final MediaType TEXT_MIME = MediaTypes.TEXT_PLAIN;

  private String bodyText;
  private String bodyHtml;
  private final Map<BMailMimeMessageHeader, String> headers = new HashMap<>();

  public BMailMimeMessage(MimeMessage mimeMessage) throws MessagingException, IOException {

    this.mimeMessage = mimeMessage;

    if (!hasNoContent()) {
      this.parseContent();
    }

  }

  /**
   * We recreate a MimeMessage at {@link BMailMimeMessage#toMimeMessage()}
   * after the user has changed the value with the setter
   * To know if it's a multipart or not, we parse the email parts
   *
   * @throws MessagingException - throw
   * @throws IOException        - throw
   */
  private void parseContent() throws MessagingException, IOException {
    /**
     * A rapid hack to extract the HTML and plain text
     * This code could check also the content type of each part but yeah.
     */
    String messageContentType = this.mimeMessage.getContentType();
    String messageMime;
    try {
      messageMime = MediaTypes.createFromMimeValue(messageContentType)
        .getExtension();
    } catch (NullValueException e) {
      throw new MessagingException("The message ContentType was null for the email message, we can't parse its content");
    }
    switch (messageMime) {
      case MediaTypeExtension.TEXT_EXTENSION:
        this.bodyText = mimeMessage.getContent().toString();
        break;
      case MediaTypeExtension.HTML_EXTENSION:
        this.bodyHtml = mimeMessage.getContent().toString();
        break;
      case "alternative":
        // ie "multipart/alternative":
        Multipart multipart = (Multipart) this.mimeMessage.getContent();
        for (int i = 0; i < multipart.getCount(); i++) {
          BodyPart bodyPart = multipart.getBodyPart(i);
          String partMime;
          try {
            partMime = MediaTypes.createFromMimeValue(bodyPart.getContentType())
              .getExtension();
          } catch (NullValueException e) {
            throw new MessagingException("The ContentType of this body part email message was null, we can't parse its content");
          }
          if (partMime.equals(MediaTypeExtension.TEXT_EXTENSION) & this.bodyText == null) {
            this.bodyText = bodyPart.getContent().toString();
          } else if (partMime.equals(MediaTypeExtension.HTML_EXTENSION) & this.bodyHtml == null) {
            this.bodyHtml = bodyPart.getContent().toString();
          } else {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            this.attachmentParts.add(attachmentPart);
          }
        }
        break;
    }
  }


  public static BMailMimeMessage createEmpty() {

    /**
     * Note: Passing a null session works also
     */
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    try {
      return new BMailMimeMessage(new MimeMessage(session));
    } catch (MessagingException | IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static BMailMimeMessage createFromBMail(BMailMimeMessage message) {

    try {
      return createFromMimeMessage(message.toMimeMessage());
    } catch (MessagingException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static BMailMimeMessage createFromRawTextNative(String message) throws IOException, MessagingException {

    message = message.replace("\r\n", "\n");
    Session session = Session.getInstance(new Properties());

    ByteArrayInputStream is = new ByteArrayInputStream(message.getBytes(emailCharset));
    MimeMessage mimeMessage = new MimeMessage(session, is);
    return createFromMimeMessage(mimeMessage);

  }

  /**
   * @param message - the mime message in string format
   * @return the BMailMessage
   * @deprecated - does not work fully (body is not captured)
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated
  public static BMailMimeMessage createFromRawTextViaMime4j(String message) throws MimeException, IOException {

    BMailMimeMessage bmailMimeMessage = BMailMimeMessage.createEmpty();
    ContentHandler handler = new BMailMimeTextMime4JParser(bmailMimeMessage);

    MimeConfig mime4jParserConfig = MimeConfig.DEFAULT;
    BodyDescriptorBuilder bodyDescriptorBuilder = new DefaultBodyDescriptorBuilder();
    MimeStreamParser parser = new MimeStreamParser(mime4jParserConfig, DecodeMonitor.SILENT, bodyDescriptorBuilder);
    parser.setContentDecoding(true);
    parser.setContentHandler(handler);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
    parser.parse(inputStream);

    return bmailMimeMessage;
  }

  @SuppressWarnings("unused")
  public BMailMimeMessage createMessageFromRawString(String emailContent) {

    ByteArrayInputStream inputStream = new ByteArrayInputStream(emailContent.getBytes(StandardCharsets.UTF_8));
    MimeMessage mimeMessage;
    try {

      mimeMessage = new MimeMessage(null, inputStream);

      return new BMailMimeMessage(mimeMessage);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  public static BMailMimeMessage createFromMimeMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
    return new BMailMimeMessage(mimeMessage);
  }

  public BMailMimeMessage setFrom(String from) {

    try {
      mimeMessage.setFrom(new InternetAddress(from));
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;

  }

  public BMailMimeMessage setFromInternetAddress(InternetAddress internetAddress) {

    try {
      mimeMessage.setFrom(internetAddress);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;

  }


  public MimeMessage toMimeMessage() {


    /**
     * We set the content back
     * <p>
     * This is the only data that can be scalar or a list
     * <p>
     * Scalar: the content is only  HTML or Text content
     * Multipart: the body text, the body html and the attachement if any
     * <p>
     * All other data are in the mime message
     */
    try {

      /**
       * https://javaee.github.io/javamail/FAQ
       * You'll want to send a MIME multipart/alternative message.
       * You construct such a message essentially the same way you construct a multipart/mixed message,
       * using a MimeMultipart object constructed using new MimeMultipart("alternative").
       * You then insert the text/plain body part as the first part in the multi-part
       * and insert the text/html body part as the second part in the multipart.
       * You'll need to construct the plain and html parts yourself to have appropriate content.
       * See RFC2046 for details of the structure of such a message.
       */
      int partCount = attachmentParts.size();
      boolean bodyPartFound = false;
      if (bodyText != null) {
        bodyPartFound = true;
        partCount++;
      }
      if (bodyHtml != null) {
        bodyPartFound = true;
        partCount++;
      }
      if (!bodyPartFound) {
        throw new RuntimeException("A body part should be present (html or txt)");
      }
      if (partCount == 1) {

        if (bodyHtml != null) {
          this.mimeMessage.setContent(bodyHtml, HTML_MIME.toString());
        }
        if (bodyText != null) {
          this.mimeMessage.setContent(bodyText, TEXT_MIME.toString());
        }

      } else {// create the Multipart and its parts to it

        Multipart mp = new MimeMultipart("alternative");

        String plain = bodyText;
        if (plain != null) {
          MimeBodyPart textBodyPart = new MimeBodyPart();
          textBodyPart.setText(plain, emailCharset.name(), TEXT_MIME.getSubType());
          mp.addBodyPart(textBodyPart);
        }

        String html = bodyHtml;
        if (html != null) {
          MimeBodyPart htmlBodyPart = new MimeBodyPart();
          htmlBodyPart.setText(html, emailCharset.name(), HTML_MIME.getSubType());
          mp.addBodyPart(htmlBodyPart);
        }

        for (MimeBodyPart attachmentPart : attachmentParts) {
          mp.addBodyPart(attachmentPart);
        }

        // add the Multipart to the message
        this.mimeMessage.setContent(mp);

      }

      /**
       * Headers
       */
      for(Map.Entry<BMailMimeMessageHeader, String> header:headers.entrySet()){
        this.mimeMessage.addHeader(header.getKey().name(), header.getValue());
      }

      // Computes additional headers
      // Updates the appropriate header fields of this message to be consistent with the message's contents.
      this.mimeMessage.saveChanges();
      return this.mimeMessage;

    } catch (
      MessagingException e) {
      throw new RuntimeException(e);
    }


  }

  public boolean hasNoContent() {
    boolean hasNoContent = false;
    try {
      this.mimeMessage.getContent();
    } catch (MessagingException | IOException e) {
      hasNoContent = true;
    }
    return hasNoContent;
  }

  public BMailMimeMessage setTo(String to) {
    try {
      this.mimeMessage.setRecipients(MimeMessage.RecipientType.TO, to);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;

  }

  public BMailMimeMessage setToInternetAddresses(List<InternetAddress> to) {
    try {
      this.mimeMessage.setRecipients(MimeMessage.RecipientType.TO, to.toArray(new InternetAddress[0]));
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;

  }

  public BMailMimeMessage setTo(String to, String toName) {
    try {
      InternetAddress toAddress = new InternetAddress(to);
      toAddress.setPersonal(toName, String.valueOf(StandardCharsets.UTF_8));
      this.mimeMessage.addRecipient(MimeMessage.RecipientType.TO, toAddress);
    } catch (MessagingException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return this;

  }

  @SuppressWarnings("unused")
  public BMailMimeMessage setBcc(String bcc) {
    try {
      this.mimeMessage.setRecipients(MimeMessage.RecipientType.BCC, InternetAddress.parse(bcc, false));
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;
  }


  @SuppressWarnings("unused")
  public BMailMimeMessage setCc(String cc) {
    try {
      this.mimeMessage.setRecipients(MimeMessage.RecipientType.CC, InternetAddress.parse(cc, false));
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public BMailMimeMessage setBodyPlainText(String bodyText) {
    assert bodyText != null;
    this.bodyText = bodyText;
    return this;
  }

  public BMailMimeMessage setBodyHtml(String bodyHtml) {
    assert bodyHtml != null;
    this.bodyHtml = bodyHtml;
    return this;
  }

  public BMailMimeMessage setSubject(String title) {
    try {
      this.mimeMessage.setSubject(title);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public String getSubject() {
    try {
      return this.mimeMessage.getSubject();
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public List<BMailInternetAddress> getTos() {
    try {
      Address[] allRecipients = this.mimeMessage.getRecipients(MimeMessage.RecipientType.TO);
      if (allRecipients == null) {
        return new ArrayList<>();
      }
      return Arrays.stream(allRecipients)
        .filter(recipientAddress -> recipientAddress instanceof InternetAddress)
        .map(recipientAddress -> (InternetAddress) recipientAddress)
        .map(BMailInternetAddress::of)
        .collect(Collectors.toList());
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public BMailAddresses getToAsAddresses() {
    try {
      return BMailAddresses.of(getTos());
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public InternetAddress getToInternetAddress() {
    List<InternetAddress> toInternetAddresses = getToInternetAddresses();
    if (toInternetAddresses.size() >= 1) {
      return toInternetAddresses.get(0);
    }
    return null;
  }

  public String getHtml() {
    return bodyHtml;

  }

  public String getPlainText() {
    return bodyText;
  }


  /**
   * @return the raw data of the message (ie mime encoding)
   * You can pipe it to `sendmail` to send it for instance
   */
  public String toEml() {

    ByteArrayOutputStream copyBoas = new ByteArrayOutputStream();
    try {
      MimeMessage mimeMessage = toMimeMessage();
      mimeMessage.writeTo(copyBoas);
      return copyBoas.toString(emailCharset);
    } catch (IOException | MessagingException e) {
      throw new RuntimeException(e);
    }

  }

  public BMailInternetAddress getFrom() {

    return BMailInternetAddress.of(getFromInternetAddress());

  }

  public String getFromAsString() {
    return getFromInternetAddress().toString();
  }

  /**
   * @param absolutePath an absolute path
   * @return the message
   * @throws MessagingException if any errors occurs
   */
  public BMailMimeMessage addAttachment(Path absolutePath) throws MessagingException {

    MimeBodyPart attachmentPart = new MimeBodyPart();

    attachmentPart.setFileName(absolutePath.getFileName().toString());
    DataHandler dataHandler;
    try {
      dataHandler = new DataHandler(absolutePath.toUri().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    attachmentPart.setDataHandler(dataHandler);
    try {
      attachmentPart.setHeader("Content-Type", MediaTypes.createFromPath(absolutePath).toString());
    } catch (NotAbsoluteException e) {
      throw new MessagingException(e.getMessage(), e);
    }
    this.attachmentParts.add(attachmentPart);
    return this;

  }

  public BMailMimeMessage setBody(String result, String subType) {

    switch (subType) {
      case "plain":
        return this.setBodyPlainText(result);
      case "html":
        return this.setBodyHtml(result);
      default:
        throw new IllegalArgumentException("The type (" + subType + ") is not a body type");
    }

  }

  @SuppressWarnings("unused")
  public int getAttachmentSize() {
    return this.attachmentParts.size();
  }

  @SuppressWarnings("unused")
  public String getContentType() {
    try {
      return this.mimeMessage.getContentType();
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  public String toEmlWithoutHeaders(String... ignoreList) {
    ByteArrayOutputStream copyBoas = new ByteArrayOutputStream();
    try {
      MimeMessage mimeMessage = toMimeMessage();
      mimeMessage.writeTo(copyBoas, ignoreList);
      return copyBoas.toString(emailCharset);
    } catch (IOException | MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the sending date
   * @throws MessagingException - if the message was not sent
   */
  public Date getSentDate() throws MessagingException {

    return this.mimeMessage.getSentDate();

  }

  public BMailMimeMessage setCcInternetAddresses(List<InternetAddress> cc) {
    try {
      this.mimeMessage.setRecipients(MimeMessage.RecipientType.CC, cc.toArray(new InternetAddress[0]));
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public BMailMimeMessage setBccInternetAddresses(List<InternetAddress> bcc) {
    try {
      this.mimeMessage.setRecipients(MimeMessage.RecipientType.BCC, bcc.toArray(new InternetAddress[0]));
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public List<InternetAddress> getToInternetAddresses() {

    try {
      List<InternetAddress> internetAddresses = new ArrayList<>();
      Address[] allRecipients = this.mimeMessage.getRecipients(MimeMessage.RecipientType.TO);
      if (allRecipients == null) {
        return internetAddresses;
      }

      for (Address address : allRecipients) {
        if (address instanceof InternetAddress) {
          internetAddresses.add((InternetAddress) address);
        }
      }
      return internetAddresses;
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }

  }

  public InternetAddress getFromInternetAddress() {
    try {
      Address[] from = this.mimeMessage.getFrom();
      if (from != null && from.length >= 1) {
        return (InternetAddress) from[0];
      } else {
        return null;
      }
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public Email toSimpleEmail() {
    return EmailConverter.mimeMessageToEmail(toMimeMessage());
  }


  public BMailMimeMessage addHeader(BMailMimeMessageHeader received, String value) {
    this.headers.put(received,value);
    return this;
  }
}
