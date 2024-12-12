package com.sistemadistribuido.server;

import com.sistemadistribuido.client.SimpleClient;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

public class TCPServer {
    private static final int SERVER_PORT = 5000;
    private static List<ClientInfo> clients = new CopyOnWriteArrayList<>();
    private static DefaultTableModel tableModel;

    private static final SystemInfo si = new SystemInfo();
    private static final CentralProcessor processor = si.getHardware().getProcessor();
    private static final GlobalMemory memory = si.getHardware().getMemory();

    private static ClientInfo serverInfo;


    public static void main(String[] args) {
        new Thread(new UDPBroadcaster()).start(); // Transmisión de la IP del servidor
        new Thread(new CandidateBroadcaster()).start(); // Transmisión de los candidatos

        SwingUtilities.invokeLater(TCPServer::createGUI);
        // Añadir al servidor a la lista de clientes
        addServerToList();
        startServerDataUpdate(); // Actualizar datos dinámicos del servidor periódicamente
        startServerEvaluation(); // Evaluar cambio de servidor cada 12 segundos

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

        tableModel = new DefaultTableModel(
                new String[]{
                        "Tipo", "IP", "Nombre", "Modelo Procesador", "Velocidad Procesador",
                        "Núcleos", "Capacidad Disco", "Versión OS", "CPU (%)",
                        "Memoria Libre", "Ancho de Banda (%)", "Disco Libre", "Estado", "Puntaje"
                }, 0
        );


        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Personalizar las celdas para diferenciar al servidor
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ("Servidor".equals(table.getValueAt(row, 0))) { // Columna "Tipo"
                    cell.setBackground(Color.LIGHT_GRAY); // Color para el servidor
                } else {
                    cell.setBackground(Color.WHITE); // Color para los clientes
                }
                return cell;
            }
        });

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
                        client.getTipo().equals("Servidor") ? "Servidor" : "Cliente", // Tipo
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

        // Ponderación de modelos de procesadores
        private static final Map<String, Integer> processorModelScores = Map.of(
                "Intel Core i7", 30,
                "Intel Core i5", 20,
                "Intel Core i3", 10,
                "AMD Ryzen 7", 30,
                "AMD Ryzen 5", 20,
                "AMD Ryzen 3", 10
        );

        // Pesos de cada componente
        private static final double WEIGHT_PROCESSOR = 0.5;
        private static final double WEIGHT_DISK = 0.2;
        private static final double WEIGHT_MEMORY = 0.2;
        private static final double WEIGHT_BANDWIDTH = 0.1;

        // Valores máximos para normalización
        private static final int MAX_CORES = 16; // Núcleos físicos
        private static final double MAX_SPEED = 5.0; // GHz
        private static final double MAX_DISK_CAPACITY = 2000.0; // GB

        public static int calculateScore(TCPServer.ClientInfo client) {
            double normalizedScore = 0;

            try {
                // Procesador: Núcleos físicos
                int cores = parseInt(client.getProcessorCores());
                double coreScore = (cores / (double) MAX_CORES) * 100;

                // Procesador: Velocidad
                double speed = Double.parseDouble(client.getProcessorSpeed().replaceAll("[^\\d.]", ""));
                double speedScore = (speed / MAX_SPEED) * 100;

                // Procesador: Modelo
                int modelScore = processorModelScores.getOrDefault(client.getProcessorModel(), 5);

                // Disco: Capacidad
                double diskCapacity = Double.parseDouble(client.getDiskCapacity().replaceAll("[^\\d.]", ""));
                double diskScore = (diskCapacity / MAX_DISK_CAPACITY) * 100;

                // Memoria libre
                double memoryFree = Double.parseDouble(client.getMemoryFree().replaceAll("[^\\d.]", ""));
                double memoryScore = memoryFree / 32.0 * 100; // Suponiendo un máximo de 32 GB de RAM

                // Ancho de banda libre
                double bandwidthFree = Double.parseDouble(client.getBandwidthFree().replaceAll("[^\\d.]", ""));
                double bandwidthScore = bandwidthFree; // Ya está en porcentaje (0-100)

                // Calcular puntaje ponderado
                normalizedScore += (modelScore + coreScore + speedScore) * WEIGHT_PROCESSOR;
                normalizedScore += diskScore * WEIGHT_DISK;
                normalizedScore += memoryScore * WEIGHT_MEMORY;
                normalizedScore += bandwidthScore * WEIGHT_BANDWIDTH;

            } catch (NumberFormatException e) {
                System.err.println("Error al calcular el puntaje: " + e.getMessage());
            }

            return (int) normalizedScore;
        }
    }



    public static class ClientInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        // Monitoreo Estático
        private String tipo;
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

        public ClientInfo(String tipo, String ip, String name, String processorModel, String processorSpeed, String processorCores,
                          String diskCapacity, String osVersion, String cpuUsage, String memoryFree,
                          String bandwidthFree, String diskFree, String connectionStatus) {
            this.tipo = tipo;
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

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
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
                    "tipo='" + tipo + '\'' +
                    ", ip='" + ip + '\'' +
                    ", name='" + name + '\'' +
                    ", processorModel='" + processorModel + '\'' +
                    ", processorSpeed='" + processorSpeed + '\'' +
                    ", processorCores='" + processorCores + '\'' +
                    ", diskCapacity='" + diskCapacity + '\'' +
                    ", osVersion='" + osVersion + '\'' +
                    ", cpuUsage='" + cpuUsage + '\'' +
                    ", memoryFree='" + memoryFree + '\'' +
                    ", bandwidthFree='" + bandwidthFree + '\'' +
                    ", diskFree='" + diskFree + '\'' +
                    ", score=" + score +
                    ", connectionStatus='" + connectionStatus + '\'' +
                    '}';
        }
    }

    private static String getProcessorModel() {
        return processor.getProcessorIdentifier().getName();
    }
    private static String getProcessorSpeed() {
        return String.format("%.2f GHz", processor.getMaxFreq() / 1_000_000_000.0);
    }
    private static int getProcessorCores() {
        return processor.getPhysicalProcessorCount();
    }
    private static String getDiskCapacity() {
        long totalDiskCapacity = 0;
        for (File root : File.listRoots()) {
            totalDiskCapacity += root.getTotalSpace();
        }
        return String.format("%.2f GB", totalDiskCapacity / (1024.0 * 1024 * 1024));
    }
    private static String getOSVersion() {
        return si.getOperatingSystem().toString();
    }
    private static long[] previousTicks = null;


    private static String getCpuUsage() {
        // Obtener los ticks actuales del CPU
        long[] currentTicks = processor.getSystemCpuLoadTicks();

        // Si no hay ticks anteriores, inicializarlos y devolver 0%
        if (previousTicks == null) {
            previousTicks = currentTicks;
            return "0.00 %";
        }

        // Calcular el uso del CPU basado en la diferencia de ticks
        double usage = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;

        // Actualizar los ticks anteriores
        previousTicks = currentTicks;

        return String.format("%.2f %%", usage);
    }

    private static String getMemoryFree() {
        double memoryFree = memory.getAvailable() / (1024.0 * 1024 * 1024);
        return String.format("%.2f GB", memoryFree);
    }

    private static String getDiskFree() {
        long totalDiskFree = 0;
        for (File root : File.listRoots()) {
            totalDiskFree += root.getFreeSpace();
        }
        return String.format("%.2f GB", totalDiskFree / (1024.0 * 1024 * 1024));
    }

    private static String getBandwidthFree() {
        // Simulación de ancho de banda libre
        return String.format("%.2f %%", Math.random() * 100);
    }
    private static void addServerToList() {
        try {
            serverInfo = getClientInfo();
            int score = ScoreCalculator.calculateScore(serverInfo);
            serverInfo.setScore(score);

            // Evitar duplicados
            synchronized (clients) {
                boolean alreadyExists = clients.stream()
                        .anyMatch(client -> client.getConnectionStatus().equals("Servidor"));
                if (!alreadyExists) {
                    clients.add(serverInfo);
                }
            }
            updateTable();
        } catch (Exception e) {
            System.err.println("Error al calcular el puntaje del servidor: " + e.getMessage());
        }
    }



    private static ClientInfo getClientInfo() throws UnknownHostException {
        String tipo = "Servidor";
        String ip = InetAddress.getLocalHost().getHostAddress();
        String name = InetAddress.getLocalHost().getHostName();
        String processorModel = getProcessorModel();
        String processorSpeed = getProcessorSpeed();
        String processorCores = String.valueOf(getProcessorCores());
        String diskCapacity = getDiskCapacity();
        String osVersion = getOSVersion();
        String cpuUsage = getCpuUsage();
        String memoryFree = getMemoryFree();
        String bandwidthFree = getBandwidthFree();
        String diskFree = getDiskFree();

        // Crear objeto ClientInfo para el servidor
        ClientInfo serverInfo = new ClientInfo(
                tipo, ip, name, processorModel, processorSpeed, processorCores, diskCapacity, osVersion,
                cpuUsage, memoryFree, bandwidthFree, diskFree, "Conectado"
        );

        return serverInfo;
    }

    private static void startServerEvaluation() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            synchronized (clients) {
                if (clients.isEmpty()) {
                    System.out.println("No hay clientes conectados para evaluar.");
                    return;
                }

                // Obtener el cliente con mayor puntaje
                ClientInfo topClient = clients.stream()
                        .sorted(Comparator.comparingInt(ClientInfo::getScore).reversed())
                        .findFirst()
                        .orElse(null);

                if (topClient == null) {
                    System.out.println("No se encontró un cliente con mayor puntaje.");
                    return;
                }

                try {

                    if(!serverInfo.getName().equals(topClient.getName())) { //validacion para omitir validacion si el unico en la lista es el servidor

                        int serverScore = serverInfo.getScore();

                        if (topClient.getScore() > serverScore && clients.size() != 1) {
                            System.out.println("Cambio de servidor: " + topClient.getIp() + " será el nuevo servidor.");
                            // El antiguo servidor se convierte en cliente
                            System.out.println("El servidor actual se convertirá en cliente.");

                            //Cerrar de manera correcta el servidor ya que la siguiente linea terminaba todo el fujo e impedia
                            // que se ejecutara ahora como cliente

                            //TCPServer.stopServer(); // Detener funciones de servidor
                            SimpleClient.main(new String[]{}); // Reinicia como cliente
                        }
                    }else {
                        System.out.println("El servidor actual sigue siendo el mejor (puntaje: " + serverInfo.getScore() + ").");
                    }
                } catch (Exception e) {
                    System.err.println("Error durante la evaluación del servidor: " + e.getMessage());
                }
            }
        }, 0, 18, TimeUnit.SECONDS); // Evaluación cada 12 segundos
    }


    public static void stopServer() {
        try {
            System.out.println("Deteniendo el servidor...");
            System.exit(0); // Detiene el proceso del servidor
        } catch (Exception e) {
            System.err.println("Error al detener el servidor: " + e.getMessage());
        }
    }

    private static void startServerDataUpdate() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                synchronized (serverInfo) {
                    // Actualizar datos dinámicos
                    serverInfo.setCpuUsage(getCpuUsage());
                    serverInfo.setMemoryFree(getMemoryFree());
                    serverInfo.setDiskFree(getDiskFree());
                    serverInfo.setBandwidthFree(getBandwidthFree());

                    // Recalcular el puntaje del servidor
                    int newScore = ScoreCalculator.calculateScore(serverInfo);
                    serverInfo.setScore(newScore);

                    // Actualizar la tabla
                    updateTable();

                    System.out.println("Datos del servidor actualizados: " + serverInfo);
                }
            } catch (Exception e) {
                System.err.println("Error al actualizar los datos del servidor: " + e.getMessage());
            }
        }, 0, 6, TimeUnit.SECONDS); // Actualizar cada 6 segundos
    }





}



