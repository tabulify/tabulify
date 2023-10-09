package net.bytle.monitor;

import net.bytle.dns.DnsResolver;
import net.bytle.dns.DnsUtil;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MonitorMain {


  public static void main(String[] args) throws TextParseException, ExecutionException, InterruptedException {

    DnsDomainData combostrap = DnsDomainData.create("combostrap.com");

    LookupSession lookupSession = LookupSession.builder()
      .resolver(DnsResolver.getLocal())
      .cache(new Cache())
      .build();
    List<DnsDomainData> domains = new ArrayList<>();
    domains.add(combostrap);
    for (DnsDomainData domain : domains) {

      LookupResult lookupResult = lookupSession.lookupAsync(domain.getName(), Type.TXT)
        .toCompletableFuture()
        .get();
      for(Record record: lookupResult.getRecords()){
        TXTRecord txtRecord = ((TXTRecord) record);
        System.out.println(DnsUtil.getStringFromTxtRecord(txtRecord));
      }
//      DnsDomain dnsDomain = DnsDomain.createFrom(domain);
//      Name name = dnsDomain.getDmarcName();
//      TXTRecord dmarcTextRecord = dnsDomain.getDmarcRecord();
//      Assert.assertNotNull("The dmarc record for the name (" + name + ") was not found", dmarcTextRecord);
//      String actualDmarcStringValue = DnsUtil.getStringFromTxtRecord(dmarcTextRecord);
//      Assert.assertEquals("The dmarc record for the name (" + name + ") should be good", expectedDMarcValue, actualDmarcStringValue);

    }

  }

}
