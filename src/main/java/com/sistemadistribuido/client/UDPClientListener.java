package com.sistemadistribuido.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPClientListener {
    private static final int BROADCAST_PORT = 9876;

    public static String discoverServer() {
        try (DatagramSocket socket = new DatagramSocket(BROADCAST_PORT)) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Esperando dirección IP del servidor...");
            socket.receive(packet);

            // Obtener la dirección IP del servidor desde el paquete recibido
            String serverIP = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Servidor detectado en IP: " + serverIP);
            return serverIP;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

