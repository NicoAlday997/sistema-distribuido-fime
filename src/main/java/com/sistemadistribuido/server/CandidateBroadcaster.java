package com.sistemadistribuido.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class CandidateBroadcaster implements Runnable {
    private static final int BROADCAST_PORT = 9877; // Puerto para candidatos
    private static final int BROADCAST_INTERVAL = 5000; // Intervalo en milisegundos

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            while (true) {
                // Obtener los mejores candidatos
                List<TCPServer.ClientInfo> topCandidates = TCPServer.getTopCandidates();

                // Crear el mensaje con los candidatos
                StringBuilder messageBuilder = new StringBuilder();
                for (TCPServer.ClientInfo candidate : topCandidates) {
                    messageBuilder.append(candidate.getIp())
                            .append(",")
                            .append(candidate.getScore())
                            .append(";");
                }

                String message = messageBuilder.toString();
                byte[] buffer = message.getBytes();

                // Enviar el mensaje por broadcast
                DatagramPacket packet = new DatagramPacket(
                        buffer,
                        buffer.length,
                        InetAddress.getByName("255.255.255.255"), // Dirección de broadcast
                        BROADCAST_PORT
                );

                socket.send(packet);
                System.out.println("Candidatos enviados por UDP: " + message);

                // Esperar antes de enviar el próximo mensaje
                Thread.sleep(BROADCAST_INTERVAL);
            }
        } catch (Exception e) {
            System.err.println("Error en CandidateBroadcaster: " + e.getMessage());
        }
    }
}
