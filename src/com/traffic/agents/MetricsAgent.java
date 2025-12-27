package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import com.traffic.environment.Environment;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MetricsAgent extends Agent {
    private long startTime;
    private int totalVehiclesSpawned = 0;
    private int totalVehiclesExited = 0;
    private double totalTravelTime = 0;
    private int collisionCount = 0;

    protected void setup() {
        startTime = System.currentTimeMillis();
        System.out.println("MetricsAgent started.");

        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                collectMetrics();
                reportMetrics();
            }
        });
    }

    private void collectMetrics() {
        Environment env = Environment.getInstance();
        // In a real system, we'd hook into Environment events.
        // For now, we estimate based on active vehicle counts.
        totalVehiclesSpawned = env.getVehiclePositions().size(); // Placeholder
    }

    private void reportMetrics() {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("--- SIMULATION METRICS [" + uptime + "s] ---");
        System.out.println("Active Vehicles: " + totalVehiclesSpawned);
        System.out.println("Incidents Active: " + Environment.getInstance().getActiveIncidents().size());

        try (FileWriter writer = new FileWriter("metrics.csv", true)) {
            writer.write(uptime + "," + totalVehiclesSpawned + "," + collisionCount + "\n");
        } catch (IOException e) {
            // Log quietly
        }
    }
}
