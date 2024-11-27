package com.sistemadistribuido.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.Serializable;
public class TCPServer {
    private static final int SERVER_PORT = 5000;
    private static List<ClientInfo> clients = new CopyOnWriteArrayList<>();
    private static DefaultTableModel tableModel;
    public static void main(String[] args) {
        new Thread(new UDPBroadcaster()).start();
        SwingUtilities.invokeLater(TCPServer::createGUI);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Servidor TCP iniciado en el puerto " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
            while (true) {
                Object data = ois.readObject();
                if (data instanceof ClientInfo) {
                    ClientInfo clientInfo = (ClientInfo) data;

                    synchronized (clients) {
                        // Actualizar información del cliente existente o añadir uno nuevo
                        Optional<ClientInfo> existingClient = clients.stream()
                                .filter(c -> c.getIp().equals(clientInfo.getIp()))
                                .findFirst();
                        if (existingClient.isPresent()) {
                            clients.remove(existingClient.get());
                        }
                        clients.add(clientInfo);
                        updateTable();
                    }

                    System.out.println("Datos actualizados del cliente: " + clientInfo);
                } else {
                    System.err.println("Datos no reconocidos recibidos del cliente.");
                }
            }
        } catch (EOFException e) {
            System.err.println("El cliente cerró la conexión: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al procesar datos del cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }


    private static void createGUI() {
        JFrame frame = new JFrame("Servidor TCP - Lista de Clientes");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tableModel = new DefaultTableModel(new String[]{"IP", "Nombre", "CPU", "RAM", "Disco"}, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        frame.add(scrollPane);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    private static void updateTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (ClientInfo client : clients) {
                tableModel.addRow(new Object[]{
                        client.getIp(),
                        client.getName(),
                        client.getCpu(),
                        client.getRam(),
                        client.getDisk()
                });
            }
        });
    }

    public static class ClientInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String ip, name, cpu, ram, disk;

        public ClientInfo(String ip, String name, String cpu, String ram, String disk) {
            this.ip = ip;
            this.name = name;
            this.cpu = cpu;
            this.ram = ram;
            this.disk = disk;
        }

        public String getIp() { return ip; }
        public String getName() { return name; }
        public String getCpu() { return cpu; }
        public String getRam() { return ram; }
        public String getDisk() { return disk; }

        @Override
        public String toString() {
            return String.format("IP: %s, Nombre: %s, CPU: %s, RAM: %s, Disco: %s", ip, name, cpu, ram, disk);
        }
    }
}
