package com.traffic.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import com.traffic.environment.Environment;
import com.traffic.model.*;

import java.awt.event.*;
import java.awt.geom.AffineTransform;

public class RoadPanel extends JPanel {
    private Environment env = Environment.getInstance();
    private double zoomFactor = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Point lastMousePos;

    private boolean showDebug = true;

    public RoadPanel() {
        setBackground(new Color(30, 30, 30));
        new Timer(50, e -> repaint()).start();

        // Debug Toggle with 'D' key
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_D) {
                    showDebug = !showDebug;
                    repaint();
                }
            }
        });

        // ... (Existing Mouse Listeners) ...

        // Zooming logic
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double oldZoom = zoomFactor;
                if (e.getWheelRotation() < 0) {
                    zoomFactor *= 1.1;
                } else {
                    zoomFactor /= 1.1;
                }
                zoomFactor = Math.max(0.1, Math.min(zoomFactor, 10.0));

                // Adjust offsets to zoom toward mouse position
                double mouseX = e.getX();
                double mouseY = e.getY();
                offsetX -= (mouseX - offsetX) * (zoomFactor / oldZoom - 1);
                offsetY -= (mouseY - offsetY) * (zoomFactor / oldZoom - 1);

                repaint();
            }
        });

        // Panning logic
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePos != null) {
                    offsetX += e.getX() - lastMousePos.x;
                    offsetY += e.getY() - lastMousePos.y;
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply Camera Transformations
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(zoomFactor, zoomFactor);

        // 1. Draw Roads and Lanes
        for (RoadSegment road : env.getRoads().values()) {
            double roadWidth = road.getLanes() * RoadSegment.LANE_WIDTH;

            // Draw Road Surface
            g2.setStroke(new BasicStroke((float) roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.GRAY);
            g2.drawLine((int) road.getStart().getX(), (int) road.getStart().getY(),
                    (int) road.getEnd().getX(), (int) road.getEnd().getY());

            // Draw Lane Markings (Dashed lines between lanes)
            if (road.getLanes() > 1) {
                g2.setStroke(
                        new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 10 }, 0));
                g2.setColor(Color.WHITE);
                double angle = road.getAngle();
                double perpAngle = angle + Math.PI / 2.0;

                for (int i = 1; i < road.getLanes(); i++) {
                    // Calculate offset for the line between lane i-1 and i
                    double offset = (i * RoadSegment.LANE_WIDTH) - (roadWidth / 2.0);
                    int xOff = (int) (Math.cos(perpAngle) * offset);
                    int yOff = (int) (Math.sin(perpAngle) * offset);

                    g2.drawLine((int) road.getStart().getX() + xOff, (int) road.getStart().getY() + yOff,
                            (int) road.getEnd().getX() + xOff, (int) road.getEnd().getY() + yOff);
                }
            } else {
                // Single lane center line
                g2.setStroke(
                        new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 10 }, 0));
                g2.setColor(Color.WHITE);
                g2.drawLine((int) road.getStart().getX(), (int) road.getStart().getY(),
                        (int) road.getEnd().getX(), (int) road.getEnd().getY());
            }
        }

        // 2. Draw Intersections
        g2.setColor(Color.DARK_GRAY);
        for (Intersection inter : env.getIntersections().values()) {
            g2.fillOval((int) inter.getPosition().getX() - 25, (int) inter.getPosition().getY() - 25, 50, 50);
        }

        // 3. Draw Traffic Lights
        for (Map.Entry<String, Position> entry : env.getLightPositions().entrySet()) {
            Position pos = entry.getValue();
            LightState state = env.getLightStates().get(entry.getKey());

            g2.setColor(Color.BLACK);
            g2.fillRect((int) pos.getX() - 10, (int) pos.getY() - 10, 20, 20);

            if (state == LightState.RED)
                g2.setColor(Color.RED);
            else if (state == LightState.YELLOW)
                g2.setColor(Color.YELLOW);
            else
                g2.setColor(Color.GREEN);

            g2.fillOval((int) pos.getX() - 6, (int) pos.getY() - 6, 12, 12);
        }

        // 4. Draw Incidents
        for (Environment.Incident inc : env.getActiveIncidents()) {
            g2.setColor(new Color(255, 200, 0, 180));
            g2.fillOval((int) inc.position.getX() - 15, (int) inc.position.getY() - 15, 30, 30);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine((int) inc.position.getX() - 10, (int) inc.position.getY() - 10, (int) inc.position.getX() + 10,
                    (int) inc.position.getY() + 10);
            g2.drawLine((int) inc.position.getX() + 10, (int) inc.position.getY() - 10, (int) inc.position.getX() - 10,
                    (int) inc.position.getY() + 10);
        }

        // 5. Draw Vehicles
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Position> entry : env.getVehiclePositions().entrySet()) {
            Position pos = entry.getValue();
            String name = entry.getKey();

            AffineTransform vehicleTransform = g2.getTransform();
            g2.translate(pos.getX(), pos.getY());

            // Calculate orientation from lane/road
            // In a more complex model we'd store 'angle' in Environment
            // For now, we'll try to guess it from the road they are on
            double angle = 0;
            // Finding road angle... (Optimization: store angle in Environment)
            for (RoadSegment r : env.getRoads().values()) {
                if (pos.distanceTo(r.getStart()) + pos.distanceTo(r.getEnd()) < r.getLength() + 5.0) {
                    angle = r.getAngle();
                    break;
                }
            }
            g2.rotate(angle);

            if (name.startsWith("Emerg_Veh_")) {
                boolean isRed = (now / 200) % 2 == 0;
                g2.setColor(isRed ? new Color(255, 50, 50) : new Color(50, 50, 255));
                g2.fillRoundRect(-15, -7, 30, 14, 5, 5);
                // Siren Glow
                g2.setColor(new Color(g2.getColor().getRed(), g2.getColor().getGreen(), g2.getColor().getBlue(), 80));
                g2.setStroke(new BasicStroke(4));
                g2.drawOval(-20, -20, 40, 40);
            } else {
                g2.setColor(new Color(0, 180, 255));
                g2.fillRoundRect(-10, -5, 20, 10, 3, 3);
            }

            // Headlights
            g2.setColor(new Color(255, 255, 200, 150));
            g2.fillOval(8, -4, 4, 3);
            g2.fillOval(8, 1, 4, 3);

            g2.setTransform(vehicleTransform); // Back to world space

            // 6. Debug Labels
            if (showDebug) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.PLAIN, 10));
                g2.drawString(name, (int) pos.getX() - 15, (int) pos.getY() - 15);
            }
        }

        // Restore transform for any non-scaled UI elements (if added later)
        g2.setTransform(oldTransform);

        // Draw Legend/Zoom Info in screen space
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Zoom: " + String.format("%.2f", zoomFactor) + "x | Drag to Pan | Press 'D' for Debug", 10,
                getHeight() - 20);
    }
}
