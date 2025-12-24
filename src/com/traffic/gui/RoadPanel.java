package com.traffic.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import com.traffic.environment.Environment;
import com.traffic.model.LightState;
import com.traffic.model.Position;

public class RoadPanel extends JPanel {
    private Environment env = Environment.getInstance();

    public RoadPanel() {
        setBackground(Color.DARK_GRAY);
        // Refresh timer
        new Timer(50, e -> repaint()).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int laneHeight = 60;
        int roadY = getHeight() / 2 - laneHeight;

        // Draw Road Lanes
        g2.setColor(Color.WHITE);
        for (int i = 0; i <= env.getNumLanes(); i++) {
            int y = roadY + i * laneHeight;
            if (i > 0 && i < env.getNumLanes()) {
                g2.setStroke(
                        new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 20 }, 0));
            } else {
                g2.setStroke(new BasicStroke(4));
            }
            g2.drawLine(0, y, getWidth(), y);
        }

        double scale = (double) getWidth() / env.getRoadLength();

        // Draw Lights
        for (Map.Entry<String, Position> entry : env.getLightPositions().entrySet()) {
            Position pos = entry.getValue();
            LightState state = env.getLightStates().get(entry.getKey());

            int lx = (int) (pos.getX() * scale);
            int ly = roadY - 40;

            g2.setColor(Color.BLACK);
            g2.fillRect(lx - 10, ly, 20, 40);

            g2.setColor(state == LightState.RED ? Color.RED : Color.GRAY);
            g2.fillOval(lx - 5, ly + 5, 10, 10);
            g2.setColor(state == LightState.YELLOW ? Color.YELLOW : Color.GRAY);
            g2.fillOval(lx - 5, ly + 15, 10, 10);
            g2.setColor(state == LightState.GREEN ? Color.GREEN : Color.GRAY);
            g2.fillOval(lx - 5, ly + 25, 10, 10);
        }

        // Draw Vehicles
        for (Map.Entry<String, Position> entry : env.getVehiclePositions().entrySet()) {
            Position pos = entry.getValue();
            int vx = (int) (pos.getX() * scale);
            int vy = roadY + pos.getLane() * laneHeight + laneHeight / 2 - 10;

            g2.setColor(Color.BLUE);
            g2.fillRect(vx - 20, vy, 40, 20);
            g2.setColor(Color.WHITE);
            g2.drawString(entry.getKey(), vx - 15, vy + 15);
        }
    }
}
