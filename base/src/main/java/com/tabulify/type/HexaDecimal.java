package com.tabulify.type;

public class HexaDecimal {
    /**
     * hexadecimal digits
     */
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    static public String toHexaDecimalViaStringBuilder(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02X", b));
        }
        return stringBuilder.toString();
    }

    /**
     * hexadecimal representation of the binary data.
     * hexlify
     * <a href="https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java">...</a>
     *
     * @param bytes - the bytes
     * @return an hexa string
     */
    static public String toHexaDecimalViaMap(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);

    }

    /**
     * @return the hexadecimal representation of the binary data. (Hexlify in Python)
     * Every byte of data is converted into the corresponding 2-digit hex representation. The resulting string is therefore twice as long as the length of data.
     */
    public static String toHexaDecimal(byte[] bytes) {
        return toHexaDecimalViaMap(bytes);
    }

    /**
     * @return the hexadecimal representation of the binary data. (Hexlify in Python)
     * <p>
     * javax.xml.bind.DatatypeConverter.printHexBinary(bytes); is deprecated in 9/10 and removed in 11
     */
    public static String printHexBinary(byte[] bytes) {
        return toHexaDecimalViaMap(bytes);
    }

}
