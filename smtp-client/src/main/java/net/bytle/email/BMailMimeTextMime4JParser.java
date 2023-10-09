package net.bytle.email;

import jakarta.mail.MessagingException;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * A class that is used to parse a raw text mime message.
 * <p>
 * See {@link BMailMimeMessage#createFromRawTextViaMime4j(String)}
 *
 * Note that this function is also native See {@link BMailMimeMessage#createFromRawTextNative(String)}
 *
 */
public class BMailMimeTextMime4JParser extends AbstractContentHandler {


  private final BMailMimeMessage bmailMimeMessage;
  private boolean inHeader = false;

  public BMailMimeTextMime4JParser(BMailMimeMessage bmailMimeMessage) {
    this.bmailMimeMessage = bmailMimeMessage;
  }

  @Override
  public void preamble(InputStream is) throws MimeException, IOException {
    super.preamble(is);
  }

  @Override
  public void endMessage() throws MimeException {
    super.endMessage();
  }

  @Override
  public void startMessage() throws MimeException {
    super.startMessage();
  }

  @Override
  public void endMultipart() throws MimeException {
    super.endMultipart();
  }

  @Override
  public void startMultipart(BodyDescriptor bd) throws MimeException {
    super.startMultipart(bd);
  }

  @Override
  public void startBodyPart() throws MimeException {
    super.startBodyPart();
  }

  @Override
  public void startHeader() {
    this.inHeader = true;
  }

  @Override
  public void endHeader() {
    this.inHeader = false;
  }

  @Override
  public void endBodyPart() throws MimeException {
    super.endBodyPart();
  }

  @Override
  public void field(Field field)  {
    if (inHeader){
      try {
        bmailMimeMessage.toMimeMessage().setHeader(field.getName(),field.getBody());
      } catch (MessagingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    String result = s.hasNext() ? s.next() : "";
    String subType = bd.getSubType();
    bmailMimeMessage.setBody(result,subType);
    super.body(bd, is);
  }

  @Override
  public void raw(InputStream is) throws MimeException, IOException {
    super.raw(is);
  }
}
