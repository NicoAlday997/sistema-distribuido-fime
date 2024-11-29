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

    private static String serverIP;

    // Sistema y hardware
    private static final SystemInfo si = new SystemInfo();
    private static final CentralProcessor processor = si.getHardware().getProcessor();
    private static final GlobalMemory memory = si.getHardware().getMemory();

    private static String getProcessorModel() {
        return processor.getProcessorIdentifier().getName();
    }
    private static String getProcessorSpeed() {
        return String.format("%.2f GHz", processor.getMaxFreq() / 1_000_000_000.0);
    }
    private static int getProcessorCores() {
        return processor.getLogicalProcessorCount();
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


    // Executor para actualización automática
    private static  ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static UDPCandidateListener candidateListener;


    public static void main(String[] args) {
        // Crear y asignar la interfaz gráfica
        clientInterface = new ClientInterface();
        SwingUtilities.invokeLater(() -> clientInterface.setVisible(true));

        serverIP = UDPClientListener.discoverServer();

        if (serverIP == null) {
            System.err.println("No se pudo detectar el servidor.");
            return;
        }

        System.out.println("Intentando conectar al servidor: " + serverIP);

        // Iniciar el listener UDP para candidatos
        String clientIp = obtenerIPCliente();
        candidateListener = new UDPCandidateListener(clientIp);
        new Thread(candidateListener).start();

        // Registrar desconexión al cerrar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(SimpleClient::disconnect));

        // Iniciar conexión con el servidor
        connectToServer(serverIP);
    }

    private static String obtenerIPCliente() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.err.println("Error al obtener la IP del cliente: " + e.getMessage());
            return "127.0.0.1"; // Fallback a localhost
        }
    }


    private static void cerrarSocketAnterior() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el socket previo: " + e.getMessage());
        }
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
            clientInterface.actualizarEstadoConexion("Conectado");

            // Iniciar el envío de datos
            reiniciarEnvioDatos();
        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
            clientInterface.actualizarEstadoConexion("Desconectado. Intentando reconectar...");
            reconnect();
        }
    }


    private static void actualizarInterfaz(String[] componentsData, String[] staticData) {
        if (clientInterface != null) {
            // Actualizar datos dinámicos
            clientInterface.actualizarDatosDinamicos(
                    componentsData[0], // CPU Usage
                    componentsData[1], // Memory Free
                    componentsData[2], // Bandwidth Free
                    componentsData[3], // Disk Free
                    staticData[0], // Client IP
                    staticData[7] //ServerIP
            );

            // Actualizar datos estáticos
            clientInterface.actualizarDatosEstaticos(
                    staticData[1], // Nombre de equipo
                    staticData[2], // Processor Model
                    staticData[3], // Processor Speed
                    staticData[4], // Processor Cores
                    staticData[5], // Disk Capacity
                    staticData[6] // OS Version
            );
        }
    }


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

    public static void disconnect() {
        isConnected = false;
        scheduler.shutdownNow(); // Detener el scheduler
        try {
            if (oos != null) oos.close();
            if (socket != null) socket.close();
            System.out.println("Conexión cerrada por el cliente.");
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }


    private static void reconnect() {
        isConnected = false;
        clientInterface.actualizarEstadoConexion("Intentando reconectar...");
        cerrarSocketAnterior(); // Asegurarse de que no haya sockets previos abiertos

        while (!isConnected) {
            try {
                // Verificar si soy el candidato a servidor
                if (candidateListener.isSoyPotencialServidor()) {
                    System.out.println("El servidor no responde. Me convierto en el nuevo servidor.");
                    disconnect(); // Cerrar cualquier recurso del cliente
                    iniciarComoServidor(); // Asumir el rol de servidor
                    return; // Salir del método
                }

                // Redescubrir la dirección del servidor si no soy el candidato
                serverIP = UDPClientListener.discoverServer();
                if (serverIP == null) {
                    System.err.println("No se pudo detectar un nuevo servidor.");
                    Thread.sleep(4000); // Esperar antes de intentar nuevamente
                    continue;
                }

                // Intentar conectarse al nuevo servidor
                socket = new Socket(serverIP, 5000);
                oos = new ObjectOutputStream(socket.getOutputStream());
                isConnected = true;
                clientInterface.actualizarEstadoConexion("Conectado");

                // Reiniciar el envío de datos
                reiniciarEnvioDatos();
            } catch (IOException e) {
                System.err.println("Reconexión fallida: " + e.getMessage());
                try {
                    Thread.sleep(4000); // Esperar antes de intentar nuevamente
                } catch (InterruptedException ignored) {}
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static void iniciarComoServidor() {
        try {
            // Detener recursos del cliente
            disconnect();

            // Mostrar mensaje de transición con cierre automático
            if (clientInterface != null) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane optionPane = new JOptionPane(
                            "Este cliente ahora será el servidor. La ventana se cerrará.",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    JDialog dialog = optionPane.createDialog(clientInterface, "Cambio a Servidor");

                    // Crear un temporizador para cerrar el diálogo automáticamente
                    Timer timer = new Timer(4000, e -> dialog.dispose());
                    timer.setRepeats(false); // Asegurarse de que el temporizador solo se ejecute una vez
                    timer.start();

                    dialog.setVisible(true); // Mostrar el diálogo
                    clientInterface.dispose(); // Cerrar la ventana del cliente después del mensaje
                });
            }

            System.out.println("Cerrando la ventana del cliente y convirtiéndome en servidor...");
            TCPServer.main(new String[]{}); // Llamar al main del servidor
        } catch (Exception e) {
            System.err.println("Error al iniciar como servidor: " + e.getMessage());
        }
    }


    private static void reiniciarEnvioDatos() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow(); // Detener el scheduler anterior si está en ejecución
        }

        // Crear un nuevo scheduler
        ScheduledExecutorService nuevoScheduler = Executors.newScheduledThreadPool(1);
        nuevoScheduler.scheduleAtFixedRate(() -> {
            try {
                if (isConnected && !socket.isClosed() && oos != null) {
                    // Dinámicos
                    String cpuUsage = getCpuUsage();
                    String memoryFree = getMemoryFree();
                    String diskFree = getDiskFree();
                    String bandwidthFree = getBandwidthFree();

                    // Estáticos
                    String[] staticData = new String[]{
                            InetAddress.getLocalHost().getHostAddress(),
                            InetAddress.getLocalHost().getHostName(),
                            getProcessorModel(),
                            getProcessorSpeed(),
                            String.valueOf(getProcessorCores()),
                            getDiskCapacity(),
                            getOSVersion(),
                            serverIP
                    };

                    // Enviar datos al servidor
                    TCPServer.ClientInfo clientInfo = new TCPServer.ClientInfo(
                            staticData[0], staticData[1], staticData[2], staticData[3],
                            staticData[4], staticData[5], staticData[6],
                            cpuUsage, memoryFree, bandwidthFree, diskFree, "Conectado"
                    );
                    oos.writeObject(clientInfo);
                    oos.flush();

                    System.out.println("Datos enviados al server...");
                    actualizarInterfaz(
                            new String[]{cpuUsage, memoryFree, bandwidthFree, diskFree},
                            staticData
                    );
                }
            } catch (IOException e) {
                System.err.println("Error al enviar datos al servidor: " + e.getMessage());
                disconnect();
                reconnect();
            }
        }, 0, 3, TimeUnit.SECONDS);

        SimpleClient.scheduler = nuevoScheduler; // Reemplazar con el nuevo scheduler
    }



}
