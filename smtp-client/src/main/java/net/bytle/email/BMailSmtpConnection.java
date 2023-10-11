package net.bytle.email;

import com.sun.mail.smtp.SMTPTransport;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.event.TransportEvent;
import jakarta.mail.event.TransportListener;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;

/**
 * This class is the socket connection and authentication
 */
public class BMailSmtpConnection implements AutoCloseable {



  private final SMTPTransport transport;


  public BMailSmtpConnection(BMailSmtpClient bMailSmtpClient) throws MessagingException {

    /**
     * Session is just a configuration object
     */
    Session smtpSession = bMailSmtpClient.getSession();
    /**
     * Other transport protocol may be implemented
     */
    if (!bMailSmtpClient.isDirectTlsConnection()) {
      transport = (SMTPTransport) smtpSession.getTransport(BMailSmtpProtocol.SMTP.toString());
    } else {
      transport = (SMTPTransport) smtpSession.getTransport(BMailSmtpProtocol.SMTPS.toString());
    }
    transport.addTransportListener(BMailTransportListener.create());
    /**
     * Ter info: The connect methods permits to change the session parameters
     * {@link Transport#connect(String, int, String, String)}}
     * they should be in the properties
     * <p>
     * t.connect("host", 25, "user", "pass");
     * <p>
     * EHLO and AUTH SMTP command are performed
     */
    transport.connect();

  }

  public void sendMessage(BMailMimeMessage bMailMessage) throws MessagingException {
    sendMessage(bMailMessage, bMailMessage.toMimeMessage().getAllRecipients());
  }

  public void sendMessage(BMailMimeMessage bMailMessage, Address[] recipients) throws MessagingException {

    if (recipients == null || recipients.length == 0) {
      throw new MessagingException("A recipient is mandatory");
    }

    MimeMessage mimeMessage = bMailMessage.toMimeMessage();
    // Computes additional headers
    mimeMessage.saveChanges();
    mimeMessage.setSentDate(new Date());

    transport.sendMessage(mimeMessage, recipients);

  }

  @Override
  public void close() throws MessagingException {
    transport.close();
  }

  @SuppressWarnings("unused")
  public void sayEhlo(String hostname) throws MessagingException {
    transport.issueCommand("EHLO " + hostname, -1);
    String ehloResponse = transport.getLastServerResponse();
    System.out.println("EHLO Response:");
    System.out.println(ehloResponse);
  }

  private static class BMailTransportListener implements TransportListener {
    public static BMailTransportListener create() {
      return new BMailTransportListener();
    }

    @Override
    public void messageDelivered(TransportEvent e) {
      System.out.println(e);
    }

    @Override
    public void messageNotDelivered(TransportEvent e) {
      System.out.println(e);
    }

    @Override
    public void messagePartiallyDelivered(TransportEvent e) {
      System.out.println(e);
    }
  }
}
