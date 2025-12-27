package com.traffic.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import com.traffic.environment.Environment;
import com.traffic.model.*;

import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

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

        // 0. Background (Grass/Field)
        g2.setColor(new Color(34, 139, 34, 40));
        g2.fillRect((int) (-offsetX / zoomFactor), (int) (-offsetY / zoomFactor),
                (int) (getWidth() / zoomFactor), (int) (getHeight() / zoomFactor));

        // 1. Draw Roads
        for (RoadSegment road : env.getRoads().values()) {
            double roadWidth = road.getLanes() * RoadSegment.LANE_WIDTH;

            if (road.isCurved()) {
                Path2D.Double path = new Path2D.Double();
                path.moveTo(road.getStart().getX(), road.getStart().getY());
                path.quadTo(road.getControlPoint().getX(), road.getControlPoint().getY(),
                        road.getEnd().getX(), road.getEnd().getY());

                // Border
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke((float) roadWidth + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);

                // Surface
                g2.setColor(new Color(60, 60, 60));
                g2.setStroke(new BasicStroke((float) roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);

                // Center line (dashed)
                if (road.getLanes() > 1) {
                    g2.setColor(Color.WHITE);
                    float[] dash = { 10.0f, 10.0f };
                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                    g2.draw(path);
                }
            } else {
                // Linear Road
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke((float) roadWidth + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) road.getStart().getX(), (int) road.getStart().getY(),
                        (int) road.getEnd().getX(), (int) road.getEnd().getY());

                g2.setColor(new Color(60, 60, 60));
                g2.setStroke(new BasicStroke((float) roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) road.getStart().getX(), (int) road.getStart().getY(),
                        (int) road.getEnd().getX(), (int) road.getEnd().getY());

                if (road.getLanes() > 1) {
                    g2.setColor(Color.WHITE);
                    float[] dash = { 10.0f, 10.0f };
                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                    g2.drawLine((int) road.getStart().getX(), (int) road.getStart().getY(),
                            (int) road.getEnd().getX(), (int) road.getEnd().getY());
                }
            }
        }

        // 1b. Draw Roundabouts (Premium Rendering)
        for (Roundabout ra : env.getRoundabouts().values()) {
            double x = ra.getCenter().getX();
            double y = ra.getCenter().getY();
            double r = ra.getRadius();
            float roadWidth = 60.0f;

            // 1. Asphalt Background
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(roadWidth + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));

            g2.setColor(new Color(60, 60, 60)); // Asphalt
            g2.setStroke(new BasicStroke(roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));

            // 2. Inner/Outer Curbs
            g2.setColor(new Color(180, 180, 180)); // Concrete Curb
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval((int) (x - r - roadWidth / 2), (int) (y - r - roadWidth / 2), (int) (r * 2 + roadWidth),
                    (int) (r * 2 + roadWidth));
            g2.drawOval((int) (x - r + roadWidth / 2), (int) (y - r + roadWidth / 2), (int) (r * 2 - roadWidth),
                    (int) (r * 2 - roadWidth));

            // 3. Lane Divider (Dashed)
            g2.setColor(new Color(255, 255, 255, 180));
            float[] dash = { 10.0f, 10.0f };
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));

            // 4. Center Island (Premium Grass)
            g2.setColor(new Color(34, 139, 34)); // Grass green
            g2.fillOval((int) (x - r + roadWidth / 2 + 2), (int) (y - r + roadWidth / 2 + 2),
                    (int) (r * 2 - roadWidth - 4), (int) (r * 2 - roadWidth - 4));

            // Inner garden/detail
            g2.setColor(new Color(20, 100, 20));
            g2.drawOval((int) (x - r + roadWidth / 2 + 5), (int) (y - r + roadWidth / 2 + 5),
                    (int) (r * 2 - roadWidth - 10), (int) (r * 2 - roadWidth - 10));
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
        }

        // 5. Draw Vehicles
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Position> entry : env.getVehiclePositions().entrySet()) {
            Position pos = entry.getValue();
            String name = entry.getKey();

            AffineTransform vehicleTransform = g2.getTransform();
            g2.translate(pos.getX(), pos.getY());

            double angle = 0;
            String roadId = env.getVehicleRoadId(name);
            if (roadId != null) {
                RoadSegment r = env.getRoads().get(roadId);
                if (r != null)
                    angle = r.getAngle();
            }
            g2.rotate(angle);

            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(-12, -7, 24, 14, 5, 5);

            if (name.startsWith("Bus_")) {
                g2.setColor(new Color(255, 200, 0)); // Bus Yellow
                g2.fillRoundRect(-18, -8, 36, 16, 4, 4);
                // Windows
                g2.setColor(new Color(50, 50, 50, 150));
                g2.fillRect(-14, -6, 28, 12);
            } else if (name.startsWith("Emerg_Veh_")) {
                boolean isRed = (now / 200) % 2 == 0;
                g2.setColor(isRed ? new Color(255, 50, 50) : new Color(50, 50, 255));
                g2.fillRoundRect(-15, -7, 30, 14, 5, 5);
            } else {
                g2.setColor(new Color(0, 180, 255));
                g2.fillRoundRect(-10, -5, 20, 10, 3, 3);
            }

            // Glass/Lights
            g2.setColor(new Color(200, 230, 255, 180));
            g2.fillRoundRect(2, -4, 5, 8, 1, 1);

            double accel = env.getVehicleAccels().getOrDefault(name, 0.0);
            if (accel < -0.01) {
                g2.setColor(new Color(255, 0, 0, 200));
                g2.fillOval(-11, -4, 3, 3);
                g2.fillOval(-11, 1, 3, 3);
            }

            g2.setTransform(vehicleTransform);
            if (showDebug) {
                g2.setColor(Color.WHITE);
                g2.drawString(name, (int) pos.getX() - 15, (int) pos.getY() - 20);
            }
        }

        g2.setTransform(oldTransform);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Zoom: " + String.format("%.2f", zoomFactor) + "x | Drag to Pan | Max Vehicles: 5", 10,
                getHeight() - 20);
    }
}
