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
        new Thread(new UDPBroadcaster()).start(); // Transmisión de la IP del servidor
        new Thread(new CandidateBroadcaster()).start(); // Transmisión de los candidatos

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

                        // Calcular el puntaje del cliente
                        int score = ScoreCalculator.calculateScore(clientInfo);
                        clientInfo.setScore(score);

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
            removerCliente(clientSocket.getInetAddress().getHostAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al procesar datos del cliente: " + e.getMessage());
            removerCliente(clientSocket.getInetAddress().getHostAddress());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }

    private static void removerCliente(String clientIp) {
        synchronized (clients) {
            clients.removeIf(client -> client.getIp().equals(clientIp));
            updateTable(); // Actualizar la tabla en la interfaz
        }
        System.out.println("Cliente eliminado: " + clientIp);
    }

    public static List<ClientInfo> getTopCandidates() {
        return clients.stream()
                .sorted(Comparator.comparingInt(ClientInfo::getScore).reversed()) // Ordenar por puntaje descendente
                .limit(3) // Tomar los tres primeros
                .toList();
    }



    private static void createGUI() {
        JFrame frame = new JFrame("Servidor - Lista de Clientes");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       // tableModel = new DefaultTableModel(new String[]{"IP", "Nombre", "CPU", "RAM", "Disco"}, 0);
        tableModel = new DefaultTableModel(
                new String[]{
                        "IP", "Nombre", "Modelo Procesador", "Velocidad Procesador",
                        "Núcleos", "Capacidad Disco", "Versión OS", "CPU (%)",
                        "Memoria Libre", "Ancho de Banda (%)", "Disco Libre", "Estado", "Puntaje"
                }, 0
        );

        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        frame.add(scrollPane);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    private static void updateTable() {
        SwingUtilities.invokeLater(() -> {
            // Ordenar por puntaje (mayor puntaje primero)
            clients.sort(Comparator.comparingInt(ClientInfo::getScore).reversed());

            // Actualizar la tabla con los clientes ordenados
            tableModel.setRowCount(0);
            for (ClientInfo client : clients) {
                tableModel.addRow(new Object[]{
                        client.getIp(),
                        client.getName(),
                        client.getProcessorModel(),
                        client.getProcessorSpeed(),
                        client.getProcessorCores(),
                        client.getDiskCapacity(),
                        client.getOsVersion(),
                        client.getCpuUsage(),
                        client.getMemoryFree(),
                        client.getBandwidthFree(),
                        client.getDiskFree(),
                        client.getConnectionStatus(),
                        client.getScore() // Mostrar el puntaje en la tabla
                });
            }
        });
    }


    public static class ScoreCalculator {

        public static int calculateScore(TCPServer.ClientInfo client) {
            int score = 0;

            try {
                // Limpieza de los datos dinámicos antes del cálculo
                String cleanedCpuUsage = client.getCpuUsage().replaceAll("[^\\d.]", "");
                String cleanedMemoryFree = client.getMemoryFree().replaceAll("[^\\d.]", "");
                String cleanedBandwidthFree = client.getBandwidthFree().replaceAll("[^\\d.]", "");
                String cleanedDiskFree = client.getDiskFree().replaceAll("[^\\d.]", "");

                // CPU disponible (2 puntos por cada 1% libre)
                score += 2 * (int) Double.parseDouble(cleanedCpuUsage);

                // RAM libre (1 punto por cada 100 MB libres)
                score += (int) Double.parseDouble(cleanedMemoryFree) / 100;

                // Disco libre (0.5 puntos por cada GB libre)
                score += (int) (Double.parseDouble(cleanedDiskFree) * 0.5);

                // Ancho de banda libre (1 punto por cada 1% libre)
                score += (int) Double.parseDouble(cleanedBandwidthFree);

            } catch (NumberFormatException e) {
                System.err.println("Error al calcular el puntaje: " + e.getMessage());
            }

            return score;
        }

    }


    public static class ClientInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        // Monitoreo Estático
        private String ip;
        private String name; // Nombre del equipo
        private String processorModel;
        private String processorSpeed;
        private String processorCores;
        private String diskCapacity;
        private String osVersion;

        // Monitoreo Dinámico
        private String cpuUsage;
        private String memoryFree;
        private String bandwidthFree;
        private String diskFree;
        private int score;
        private String connectionStatus; // Conectado/Desconectado

        public ClientInfo(String ip, String name, String processorModel, String processorSpeed, String processorCores,
                          String diskCapacity, String osVersion, String cpuUsage, String memoryFree,
                          String bandwidthFree, String diskFree, String connectionStatus) {
            this.ip = ip;
            this.name = name;
            this.processorModel = processorModel;
            this.processorSpeed = processorSpeed;
            this.processorCores = processorCores;
            this.diskCapacity = diskCapacity;
            this.osVersion = osVersion;
            this.cpuUsage = cpuUsage;
            this.memoryFree = memoryFree;
            this.bandwidthFree = bandwidthFree;
            this.diskFree = diskFree;
            this.connectionStatus = connectionStatus;
        }

        public ClientInfo(String ip, String name, String processorModel, String processorSpeed, String processorCores,
                          String diskCapacity, String osVersion, String cpuUsage, String memoryFree,
                          String bandwidthFree, String diskFree, int score, String connectionStatus) {
            this.ip = ip;
            this.name = name;
            this.processorModel = processorModel;
            this.processorSpeed = processorSpeed;
            this.processorCores = processorCores;
            this.diskCapacity = diskCapacity;
            this.osVersion = osVersion;
            this.cpuUsage = cpuUsage;
            this.memoryFree = memoryFree;
            this.bandwidthFree = bandwidthFree;
            this.diskFree = diskFree;
            this.score = score;
            this.connectionStatus = connectionStatus;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProcessorModel() {
            return processorModel;
        }

        public void setProcessorModel(String processorModel) {
            this.processorModel = processorModel;
        }

        public String getProcessorSpeed() {
            return processorSpeed;
        }

        public void setProcessorSpeed(String processorSpeed) {
            this.processorSpeed = processorSpeed;
        }

        public String getProcessorCores() {
            return processorCores;
        }

        public void setProcessorCores(String processorCores) {
            this.processorCores = processorCores;
        }

        public String getDiskCapacity() {
            return diskCapacity;
        }

        public void setDiskCapacity(String diskCapacity) {
            this.diskCapacity = diskCapacity;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getCpuUsage() {
            return cpuUsage;
        }

        public void setCpuUsage(String cpuUsage) {
            this.cpuUsage = cpuUsage;
        }

        public String getMemoryFree() {
            return memoryFree;
        }

        public void setMemoryFree(String memoryFree) {
            this.memoryFree = memoryFree;
        }

        public String getBandwidthFree() {
            return bandwidthFree;
        }

        public void setBandwidthFree(String bandwidthFree) {
            this.bandwidthFree = bandwidthFree;
        }

        public String getDiskFree() {
            return diskFree;
        }

        public void setDiskFree(String diskFree) {
            this.diskFree = diskFree;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getConnectionStatus() {
            return connectionStatus;
        }

        public void setConnectionStatus(String connectionStatus) {
            this.connectionStatus = connectionStatus;
        }

        @Override
        public String toString() {
            return "ClientInfo{" +
                    "ip='" + ip + '\'' +
                    ", name='" + name + '\'' +
                    ", processorModel='" + processorModel + '\'' +
                    ", processorSpeed='" + processorSpeed + '\'' +
                    ", processorCores=" + processorCores +
                    ", diskCapacity='" + diskCapacity + '\'' +
                    ", osVersion='" + osVersion + '\'' +
                    ", cpuUsage='" + cpuUsage + '\'' +
                    ", memoryFree='" + memoryFree + '\'' +
                    ", bandwidthFree='" + bandwidthFree + '\'' +
                    ", diskFree='" + diskFree + '\'' +
                    ", connectionStatus='" + connectionStatus + '\'' +
                    '}';
        }
    }

}



