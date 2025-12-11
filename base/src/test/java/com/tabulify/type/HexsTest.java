package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;

public class HexsTest {

    @Test
    public void toBytesTest() {

        String hex = Hexs.getRandomHex(10);
        System.out.println(hex.toUpperCase());
        byte[] decryptedBitString1 = parseHexBinary(hex);
        byte[] decryptedBitString2 = Hexs.toBytes(hex);
        Assert.assertArrayEquals("The two function are giving the same bit string",decryptedBitString1, decryptedBitString2);

    }

}
