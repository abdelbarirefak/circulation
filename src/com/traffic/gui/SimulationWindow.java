package com.traffic.gui;

import javax.swing.*;
import java.awt.*;

public class SimulationWindow extends JFrame {
    public SimulationWindow() {
        setTitle("JADE Traffic Simulation");
        setSize(1200, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(new RoadPanel(), BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
