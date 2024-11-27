package com.sistemadistribuido.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPBroadcaster implements Runnable {
    private static final int BROADCAST_PORT = 9876;
    private static final int BROADCAST_INTERVAL = 5000; // Intervalo en milisegundos

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String serverIP = InetAddress.getLocalHost().getHostAddress();
            byte[] buffer = serverIP.getBytes();

            System.out.println("Enviando dirección IP por UDP: " + serverIP);
            while (true) {
                DatagramPacket packet = new DatagramPacket(
                        buffer,
                        buffer.length,
                        InetAddress.getByName("255.255.255.255"), // Dirección de broadcast
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
