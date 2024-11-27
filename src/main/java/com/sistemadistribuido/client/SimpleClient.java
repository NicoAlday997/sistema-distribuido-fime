package com.sistemadistribuido.client;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sistemadistribuido.server.TCPServer;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

public class SimpleClient {

    private static boolean isConnected = false;
    private static ClientInterface clientInterface;

    private static Socket socket;
    private static ObjectOutputStream oos;

    // Sistema y hardware
    private static final SystemInfo si = new SystemInfo();
    private static final CentralProcessor processor = si.getHardware().getProcessor();
    private static final GlobalMemory memory = si.getHardware().getMemory();

    // Executor para actualización automática
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        String serverIP = UDPClientListener.discoverServer();
        if (serverIP == null) {
            System.err.println("No se pudo detectar el servidor.");
            return;
        }

        System.out.println("Intentando conectar al servidor: " + serverIP);

        // Crear y asignar la interfaz gráfica
        clientInterface = new ClientInterface(serverIP);
        SwingUtilities.invokeLater(() -> clientInterface.setVisible(true));

        // Registrar desconexión al cerrar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(SimpleClient::disconnect));

        // Iniciar conexión con el servidor
        connectToServer(serverIP);
    }

    private static void connectToServer(String serverIP) {
        if (isConnected) {
            System.out.println("Ya conectado al servidor: " + serverIP);
            return;
        }

        try {
            socket = new Socket(serverIP, 5000);
            oos = new ObjectOutputStream(socket.getOutputStream());
            isConnected = true;

            // Programar actualización automática
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (isConnected && !socket.isClosed() && oos != null) {
                        // Obtener los datos
                        String cpuUsage = getCpuUsage();
                        String memoryFree = getMemoryFree();
                        String diskFree = getDiskFree();
                        String bandwidthFree = getBandwidthFree();

                        // Crear y enviar el objeto ClientInfo
                        TCPServer.ClientInfo clientInfo = new TCPServer.ClientInfo(
                                InetAddress.getLocalHost().getHostAddress(),
                                InetAddress.getLocalHost().getHostName(),
                                cpuUsage,
                                memoryFree,
                                diskFree
                        );

                        System.out.println("Enviando datos al servidor: " + clientInfo);

                        // Enviar al servidor
                        oos.writeObject(clientInfo);
                        oos.flush();

                        // Actualizar la interfaz del cliente
                        actualizarInterfaz(new String[]{cpuUsage, memoryFree, bandwidthFree, diskFree});
                    }
                } catch (SocketException e) {
                    System.err.println("Error al escribir en el socket: " + e.getMessage());
                    disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, 3, TimeUnit.SECONDS);


        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    private static void actualizarInterfaz(String[] componentsData) {
        if (clientInterface != null) {
            clientInterface.actualizarDatos(
                    componentsData[0], // CPU Usage
                    componentsData[1], // Memory Free
                    componentsData[2], // Bandwidth Free
                    componentsData[3]  // Disk Free
            );
        }
    }

    private static String getCpuUsage() {
        double usage = processor.getSystemCpuLoadBetweenTicks(processor.getSystemCpuLoadTicks()) * 100;
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

    public static void disconnect() {
        isConnected = false;
        scheduler.shutdown();
        try {
            if (oos != null) oos.close();
            if (socket != null) socket.close();
            System.out.println("Conexión cerrada por el cliente.");
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
