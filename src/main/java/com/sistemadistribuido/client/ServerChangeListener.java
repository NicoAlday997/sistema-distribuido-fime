package com.sistemadistribuido.client;

import com.sistemadistribuido.server.TCPServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ServerChangeListener implements Runnable {
    private static final int NOTIFICATION_PORT = 5001;
    private static String currentServerIP;
    private static List<TCPServer.ClientInfo> clientPriorityList;

    public static void start(String initialServerIP) {
        currentServerIP = initialServerIP;
        new Thread(new ServerChangeListener()).start();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(NOTIFICATION_PORT)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    Object data = ois.readObject();
                    if (data instanceof String) {
                        // Actualización del servidor
                        currentServerIP = (String) data;
                        System.out.println("Nuevo servidor detectado: " + currentServerIP);
                    } else if (data instanceof List) {
                        // Actualización de la lista de clientes
                        clientPriorityList = (List<TCPServer.ClientInfo>) data;
                        System.out.println("Lista de clientes actualizada.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentServerIP() {
        return currentServerIP;
    }

    public static List<TCPServer.ClientInfo> getClientPriorityList() {
        return clientPriorityList;
    }
}

