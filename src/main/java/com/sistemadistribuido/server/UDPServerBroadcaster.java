package com.sistemadistribuido.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServerBroadcaster {
    private static final int BROADCAST_PORT = 9876;
    private static final int BROADCAST_INTERVAL = 5000; // Cada 5 segundos

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String serverIP = InetAddress.getLocalHost().getHostAddress();
            byte[] buffer = serverIP.getBytes();

            System.out.println("Enviando dirección IP: " + serverIP);
            while (true) {
                DatagramPacket packet = new DatagramPacket(
                        buffer,
                        buffer.length,
                        InetAddress.getByName("255.255.255.255"),
                        BROADCAST_PORT
                );
                socket.send(packet);
                System.out.println("IP enviada a la red local.");
                Thread.sleep(BROADCAST_INTERVAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

