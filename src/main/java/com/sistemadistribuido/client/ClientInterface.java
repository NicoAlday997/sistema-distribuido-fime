package com.sistemadistribuido.client;


import javax.swing.*;
import java.awt.*;

public class ClientInterface extends JFrame {

    private JLabel lblCpuUsage;
    private JLabel lblMemoryFree;
    private JLabel lblBandwidthFree;
    private JLabel lblDiskFree;
    private JLabel lblClientIp;
    private JLabel lblServerIp;

    public ClientInterface(String serverIP) {
        setTitle("Especificaciones del Cliente");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(7, 2));

        // Crear etiquetas para los datos
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

        add(new JLabel("IP del Cliente:"));
        lblClientIp = new JLabel("Cargando...");
        add(lblClientIp);

        add(new JLabel("IP del Servidor:"));
        lblServerIp = new JLabel(serverIP);
        add(lblServerIp);

        setVisible(true);
    }

    /**
     * Actualiza los datos mostrados en la interfaz gráfica.
     *
     * @param cpuUsage      Uso del procesador (%).
     * @param memoryFree    Memoria libre (GB).
     * @param bandwidthFree Ancho de banda libre (%).
     * @param diskFree      Espacio libre en disco (GB).
     */
    public void actualizarDatos(String cpuUsage, String memoryFree, String bandwidthFree, String diskFree) {
        SwingUtilities.invokeLater(() -> {
            lblCpuUsage.setText(cpuUsage);
            lblMemoryFree.setText(memoryFree);
            lblBandwidthFree.setText(bandwidthFree);
            lblDiskFree.setText(diskFree);
            try {
                lblClientIp.setText(java.net.InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                lblClientIp.setText("Error al obtener IP");
                e.printStackTrace();
            }
        });
    }
}