package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static com.tabulify.type.Bytes.toHexaDecimalViaMap;

public class BytesTest {


    @Test
    public void bitStringToHexTest() {

        byte[] bytes = new byte[] {
                (byte) 0x01, (byte) 0xFF, (byte) 0x2E, (byte) 0x6E, (byte) 0x30
        };
        String hexStringFunc1 = printHexBinary(bytes);
        String hexStringFun1 = toHexaDecimalViaMap(bytes);
        Assert.assertEquals("The two function are giving the same hexString",hexStringFunc1, hexStringFun1);
        System.out.println(hexStringFunc1);
        byte[] decryptedBitString1 = parseHexBinary(hexStringFun1);
        byte[] decryptedBitString2 = Hexs.toBytes(hexStringFun1);
        Assert.assertArrayEquals("The two function are giving the same bit string",decryptedBitString1, decryptedBitString2);

    }

    @Test
    public void toStringTest() {
        //byte[] bytes = Bytes.getRandomBytes(10);
        byte[] bytes = new byte[] { -1, -128, 1, 127 };

        System.out.println(Bytes.toString(bytes));
    }
}
