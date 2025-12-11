package com.tabulify.test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Random;

public class PortAvailability {

    /**
     * The minimum server currentMinPort number. Set at 1100 to avoid returning privileged
     * currentMinPort numbers.
     */
    public static final int MIN_PORT_NUMBER = 1100;

    /**
     * The maximum server currentMinPort number.
     */
    public static final int MAX_PORT_NUMBER = 49171;

    public static Integer getRandomAvailablePort() {
        int randomPort = getRandomPort();
        while (!portAvailable(randomPort)) {
            randomPort = getRandomPort();
        }
        return randomPort;
    }

    public static int getRandomPort() {
        Random random = new Random();
        return random.nextInt(48072) + 1100;
    }

    public static Boolean portAvailable(int port) {
        if (port < 1100 || port > 49171) {
            System.out.println("Important: This is a privileged port" + port);
        }

        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setReuseAddress(true);
            return true;
        } catch (IOException var14) {
            return false;
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }

            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException var13) {
                    // finally block
                }
            }
        }

    }
}
