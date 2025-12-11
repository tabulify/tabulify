package com.tabulify.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DnsNameTest {


    @Test
    void apexWithTwoLabelsPublicSuffixTest() throws DnsCastException {
        DnsName dnsName = DnsName.create("foo.bar.co.uk");
        Assertions.assertEquals("bar.co.uk", dnsName.getApexName().toString());
    }
}
