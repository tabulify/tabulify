package net.bytle.os;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Properties;

public class Oss {


  /**
   * The minimum server currentMinPort number. Set at 1100 to avoid returning privileged
   * currentMinPort numbers.
   */
  public static final int MIN_PORT_NUMBER = 1100;

  /**
   * The maximum server currentMinPort number.
   */
  public static final int MAX_PORT_NUMBER = 49171;

  public static final int WIN = 1;
  public static final int LINUX = 2;

  private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

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
    for (String key : properties.stringPropertyNames()) {
      System.out.println("The key (" + key + ") has the value (" + properties.getProperty(key) + ")");
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

  public static String getVersion() {
    return System.getProperty("os.version");
  }

  public static String getName() {
    return OS_NAME;
  }


  public static Integer getType() {
    if (isWindows()) {
      return WIN;
    } else if (isUnix()) {
      return LINUX;
    } else {
      return 0;
    }
  }


  /**
   * Checks to see if a specific port is available.
   *
   * @param port the port to check for availability
   *             <p>
   *             They are checking the DatagramSocket as well to check if the port is available in UDP and TCP.
   * @see <a href="https://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java">Stack</a>
   * @see <a href="http://svn.apache.org/viewvc/camel/trunk/components/camel-test/src/main/java/org/apache/camel/test/AvailablePortFinder.java?view=markup#l130">PortFinder</a>
   */
  public static Boolean portAvailable(int port) {

    if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
      System.out.println("Important: This is a privileged port" + port);
    }

    ServerSocket ss = null;
    DatagramSocket ds = null;
    try {
      ss = new ServerSocket(port);
      ss.setReuseAddress(true);
      ds = new DatagramSocket(port);
      ds.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      if (ds != null) {
        ds.close();
      }

      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
          /* should not be thrown */
        }
      }
    }

  }

  public static String getFqdn() throws UnknownHostException {
    return InetAddress.getLocalHost().getCanonicalHostName();
  }

  public static String getUser() {
    return System.getProperty("user.name");
  }

}
