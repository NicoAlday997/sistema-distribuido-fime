package com.sistemadistribuido.client;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class UDPCandidateListener implements Runnable {
    private static final int BROADCAST_PORT = 9877; // Puerto para candidatos
    private final String clientIp;
    private boolean soyPotencialServidor = false;
    private boolean soySegundoServidor = false;

    public UDPCandidateListener(String clientIp) {
        this.clientIp = clientIp;
    }
    public boolean isSoyPotencialServidor() {
        return soyPotencialServidor;
    }
    public boolean isSoySegundoServidor() {
        return soySegundoServidor;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(BROADCAST_PORT)) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                // Escuchar mensaje UDP
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Mensaje recibido con candidatos: " + message);

                // Procesar la lista de candidatos
                processCandidates(message);
            }
        } catch (Exception e) {
            System.err.println("Error en UDPCandidateListener: " + e.getMessage());
        }
    }

    private void processCandidates(String message) {
        // Reiniciar estados
        soyPotencialServidor = false;
        soySegundoServidor = false;

        // Separar candidatos
        String[] candidates = message.split(";");
        for (int i = 0; i < candidates.length; i++) {
            String[] candidateData = candidates[i].split(",");
            if (candidateData.length == 2) {
                String candidateIp = candidateData[0];
                int candidateScore = Integer.parseInt(candidateData[1]);

                // Verificar si el cliente es uno de los candidatos
                if (clientIp.equals(candidateIp)) {
                    if (i == 0) {
                        soyPotencialServidor = true; // Primer lugar
                        System.out.println("Soy el primer candidato (IP: " + clientIp + ", Puntaje: " + candidateScore + ")");
                    } else if (i == 1) {
                        soySegundoServidor = true; // Segundo lugar
                        System.out.println("Soy el segundo candidato (IP: " + clientIp + ", Puntaje: " + candidateScore + ")");
                    }
                }
            }
        }
    }
}
