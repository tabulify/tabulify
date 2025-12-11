package com.tabulify.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DnsPublicSuffixTest {

    @Test
    void publicSuffixTest() throws DnsCastException {
        boolean uk = DnsPublicSuffix.isPublicSuffix("uk");
        Assertions.assertTrue(uk);
        boolean couk = DnsPublicSuffix.isPublicSuffix("co.uk");
        Assertions.assertTrue(couk);
        //System.out.println(DnsPublicSuffix.getPublicSuffixes());
    }
}
