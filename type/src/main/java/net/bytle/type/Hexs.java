package net.bytle.type;

import java.util.Random;

public class Hexs {


    /**
     * hexadecimal digits
     */
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static byte[] toBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     *
     * @param length
     * @return a random hexadecimal of length length
     */
    public static String getRandomHex(int length){
        Random rand = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0;i<length;i++) {
            int myRandomNumber = rand.nextInt(hexArray.length); // Generates a random number between 0 and 16
            // Tip: we could have use:
            // System.out.printf("%x\n",myRandomNumber); // Prints it in hex, such as "0x14"
            stringBuilder.append(Integer.toHexString(myRandomNumber));
        }
        return stringBuilder.toString();
    }

}
