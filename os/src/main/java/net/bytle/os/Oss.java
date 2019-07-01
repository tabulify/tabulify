package net.bytle.os;

import java.util.Properties;

public class Oss {

    public static final int WIN = 1;
    public static final int LINUX = 2;

    private static String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static void printInfo() {

        System.out.println(OS_NAME);

        if (isWindows()) {
            System.out.println("This is Windows");
        } else if (isMac()) {
            System.out.println("This is Mac");
        } else if (isUnix()) {
            System.out.println("This is Unix or Linux");
        } else if (isSolaris()) {
            System.out.println("This is Solaris");
        } else {
            System.out.println("The OS is not known");
        }

        final Properties properties = System.getProperties();
        for (String key: properties.stringPropertyNames()){
            System.out.println ("The key ("+key+") has the value ("+properties.getProperty(key)+")");
        }
    }

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static boolean isUnix() {
        return (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix"));
    }

    public static boolean isSolaris() {
        return OS_NAME.contains("sunos");
    }

    public static String getVersion(){
        return System.getProperty("os.version");
    }

    public static String getName(){
        return OS_NAME;
    }


    public static Integer getType() {
        if (isWindows()) {
            return WIN;
        } else if (isUnix()){
            return LINUX;
        } else {
            return 0;
        }
    }
}