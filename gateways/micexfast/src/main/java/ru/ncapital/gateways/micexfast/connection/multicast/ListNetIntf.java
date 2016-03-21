package ru.ncapital.gateways.micexfast.connection.multicast;

import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.out;

/**
 * Created by egore on 12/22/15.
 */
public class ListNetIntf {

        public static void main(String args[]) throws SocketException {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
                displayInterfaceInformation(netint);
        }

        static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
            out.printf("Display name: %s\n", netint.getDisplayName());
            out.printf("Name: %s\n", netint.getName());
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                out.printf("InetAddress: %s\n", inetAddress);
            }
            out.printf("\n");
        }
    }