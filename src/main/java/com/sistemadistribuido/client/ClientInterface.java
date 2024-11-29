package com.sistemadistribuido.client;

import javax.swing.*;
import java.awt.*;

public class ClientInterface extends JFrame {

    // Etiquetas para monitoreo estático

    private JLabel lblNombreEquipo;
    private JLabel lblProcessorModel;
    private JLabel lblProcessorSpeed;
    private JLabel lblProcessorCores;
    private JLabel lblDiskCapacity;
    private JLabel lblOSVersion;

    // Etiquetas para monitoreo dinámico
    private JLabel lblCpuUsage;
    private JLabel lblMemoryFree;
    private JLabel lblBandwidthFree;
    private JLabel lblDiskFree;
    private JLabel lblClientIp;
    private JLabel lblServerIp;
    private JLabel lblConnectionStatus;

    public ClientInterface() {
        setTitle("Especificaciones del Cliente");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(13, 2, 5, 5)); // 12 filas para todos los datos

        // Monitoreo estático
        add(new JLabel("Nombre equipo:"));
        lblNombreEquipo = new JLabel("Cargando...");
        add(lblNombreEquipo);

        add(new JLabel("Modelo del Procesador:"));
        lblProcessorModel = new JLabel("Cargando...");
        add(lblProcessorModel);

        add(new JLabel("Velocidad del Procesador (GHz):"));
        lblProcessorSpeed = new JLabel("Cargando...");
        add(lblProcessorSpeed);

        add(new JLabel("Número de Núcleos:"));
        lblProcessorCores = new JLabel("Cargando...");
        add(lblProcessorCores);

        add(new JLabel("Capacidad del Disco (GB):"));
        lblDiskCapacity = new JLabel("Cargando...");
        add(lblDiskCapacity);

        add(new JLabel("Versión del Sistema Operativo:"));
        lblOSVersion = new JLabel("Cargando...");
        add(lblOSVersion);

        // Monitoreo dinámico
        add(new JLabel("Uso del Procesador (%):"));
        lblCpuUsage = new JLabel("Cargando...");
        add(lblCpuUsage);

        add(new JLabel("Memoria Libre (GB):"));
        lblMemoryFree = new JLabel("Cargando...");
        add(lblMemoryFree);

        add(new JLabel("Ancho de Banda Libre (%):"));
        lblBandwidthFree = new JLabel("Cargando...");
        add(lblBandwidthFree);

        add(new JLabel("Espacio Libre en Disco (GB):"));
        lblDiskFree = new JLabel("Cargando...");
        add(lblDiskFree);

        // Información de red
        add(new JLabel("IP del Cliente:"));
        lblClientIp = new JLabel("Cargando...");
        add(lblClientIp);

        add(new JLabel("IP del Servidor:"));
        lblServerIp = new JLabel("Cargando...");
        add(lblServerIp);

        // Estado de conexión
        add(new JLabel("Estado de Conexión:"));
        lblConnectionStatus = new JLabel("Cargando...");
        add(lblConnectionStatus);

        setVisible(true);
    }

    /**
     * Actualiza los datos estáticos mostrados en la interfaz.
     *
     * @param processorModel Modelo del procesador.
     * @param processorSpeed Velocidad del procesador.
     * @param processorCores Número de núcleos del procesador.
     * @param diskCapacity   Capacidad total del disco.
     * @param osVersion      Versión del sistema operativo.
     */
    public void actualizarDatosEstaticos(String nombreEquipo, String processorModel, String processorSpeed, String processorCores,
                                         String diskCapacity, String osVersion) {
        SwingUtilities.invokeLater(() -> {
            lblNombreEquipo.setText(nombreEquipo);
            lblProcessorModel.setText(processorModel);
            lblProcessorSpeed.setText(processorSpeed);
            lblProcessorCores.setText(processorCores);
            lblDiskCapacity.setText(diskCapacity);
            lblOSVersion.setText(osVersion);
        });
    }

    public void actualizarEstadoConexion(String estado) {
        SwingUtilities.invokeLater(() -> lblConnectionStatus.setText(estado));
    }

    /**
     * Actualiza los datos dinámicos mostrados en la interfaz.
     *
     * @param cpuUsage      Uso del procesador.
     * @param memoryFree    Memoria libre.
     * @param bandwidthFree Ancho de banda libre.
     * @param diskFree      Espacio libre en disco.
     * @param clientIp      Dirección IP del cliente.
     */
    public void actualizarDatosDinamicos(String cpuUsage, String memoryFree, String bandwidthFree,
                                         String diskFree, String clientIp, String serverIP) {
        SwingUtilities.invokeLater(() -> {
            lblCpuUsage.setText(cpuUsage);
            lblMemoryFree.setText(memoryFree);
            lblBandwidthFree.setText(bandwidthFree);
            lblDiskFree.setText(diskFree);
            lblClientIp.setText(clientIp);
            lblServerIp.setText(serverIP);
        });
    }
}
