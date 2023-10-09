package net.bytle.email;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.ImmutableList;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * A wrapper
 */
public class BMailGMailServer {


  private final Gmail gmail;


  public BMailGMailServer() throws GeneralSecurityException, IOException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    this.gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
      .setApplicationName(APPLICATION_NAME)
      .build();
  }

  /**
   * Application name.
   */
  private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Directory to store authorization tokens for this application.
   */
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  private static final List<String> SCOPES = ImmutableList.of(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_SEND);
  private static final String CREDENTIALS_FILE_PATH = "/client_identity/gmail.json";

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
    throws IOException {
    // Load client secrets.
    InputStream in = BMailGMailServer.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets =
      GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
      HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
      .setAccessType("offline")
      .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    //returns an authorized Credential object.
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }


  public static BMailGMailServer create() throws GeneralSecurityException, IOException {
    return new BMailGMailServer();
  }

  public void printLabels() throws IOException {
    // Print the labels in the user's account.
    String user = "me";
    ListLabelsResponse listResponse = gmail.users().labels().list(user).execute();
    List<Label> labels = listResponse.getLabels();
    if (labels.isEmpty()) {
      System.out.println("No labels found.");
    } else {
      System.out.println("Labels:");
      for (Label label : labels) {
        System.out.printf("- %s\n", label.getName());
      }
    }
  }




  @SuppressWarnings("unused")
  private Gmail getGmail() {
    return this.gmail;
  }

  public Message send(BMailMimeMessage bMailMimeMessage) throws MessagingException, IOException {


    Message message = this.getGmailMessageFromBMail(bMailMimeMessage);
    message = gmail.users().messages().send("me", message).execute();
    System.out.println("Message id: " + message.getId());
    System.out.println(message.toPrettyString());
    return message;

  }
  /**
   * @return the gmail mime format message
   * @throws MessagingException if any error
   * @throws IOException if an io exception
   */
  private Message getGmailMessageFromMimeMessage(MimeMessage mimeMessage) throws MessagingException, IOException {

    Message gmailMimeMessage = new Message();
    gmailMimeMessage.setRaw(this.getMimeMessageAsGmailEncodedString(mimeMessage));
    return gmailMimeMessage;

  }

  private String getMimeMessageAsGmailEncodedString(MimeMessage email) throws MessagingException, IOException {

    // Encode and wrap the MIME message into a gmail message
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    email.writeTo(buffer);
    byte[] rawMessageBytes = buffer.toByteArray();
    return Base64.encodeBase64URLSafeString(rawMessageBytes);

  }

  @SuppressWarnings("unused")
  public Draft saveAsDraft(BMailMimeMessage bMailMimeMessage) throws IOException, MessagingException {

    // Create the draft message
    Draft draft = new Draft();
    draft.setMessage(this.getGmailMessageFromBMail(bMailMimeMessage));
    draft = this.gmail.users().drafts().create("me", draft).execute();
    System.out.println("Draft id: " + draft.getId());
    System.out.println(draft.toPrettyString());
    return draft;

  }

  private Message getGmailMessageFromBMail(BMailMimeMessage bMailMimeMessage) throws MessagingException, IOException {
    return getGmailMessageFromMimeMessage(bMailMimeMessage.toMimeMessage());
  }
}
