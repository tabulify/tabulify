package net.bytle.tower.util;

import io.vertx.ext.web.client.WebClient;
import jakarta.mail.internet.AddressException;
import net.bytle.dns.*;
import net.bytle.email.BMailInternetAddress;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.HttpClientBuilder;
import org.xbill.DNS.MXRecord;

import java.util.List;
import java.util.Set;

public class EmailAddressValidator {


  private final WebClient webClient;

  public EmailAddressValidator(EraldyApiApp eraldyApiApp) {
    webClient = HttpClientBuilder.builder(eraldyApiApp.getApexDomain().getHttpServer().getServer().getVertx())
      .buildWebClient();
  }


  public void validate(String email) throws DnsException, EmailAddressNotValid {
    BMailInternetAddress mail;
    try {
      mail = BMailInternetAddress.of(email);
    } catch (AddressException e) {
      throw new EmailAddressNotValid("The email (" + email + ") is not valid",e);
    }
    DnsSession dnsSession = DnsSession.builder().build();
    String domain = mail.getDomain();
    DnsName name;
    try {
      name = dnsSession.createDnsName(domain);
    } catch (DnsIllegalArgumentException e) {
      throw new RuntimeException("Internal error: The domain (" + domain + ") is not valid. The email is valid, The domain should be valid",e);
    }
    String noMxRecordMessage = "The domain (" + name + ") has no MX records";
    try {
      List<MXRecord> records = name.getMxRecords();
      if (records.size() == 0) {
        throw new EmailAddressNotValid(noMxRecordMessage);
      }
    } catch (DnsNotFoundException e) {
      throw new EmailAddressNotValid(noMxRecordMessage,e);
    }
    String noARecordMessage = "The domain (" + name + ") has no A records";
    try {
      Set<DnsIp> aRecords = name.getARecords();
      if (aRecords.size() == 0) {
        throw new EmailAddressNotValid(noARecordMessage);
      }
    } catch (DnsNotFoundException e) {
      throw new EmailAddressNotValid(noARecordMessage,e);
    }
    // https with valid certificate at minima
    // content: one image at minima
    // example:
    // http://take-ur-vites.org/

    // 163.com (tou522884141@163.com, ...)
    // http://poker40.com/
    // isaymur7rw5@bigmir.net
    // gsalike@mail.ru

  }
}
