package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import com.traffic.environment.Environment;
import com.traffic.model.RoadSegment;
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
    private Map<String, Double> roadCongestionHistory = new HashMap<>();
    private final double ALPHA = 0.2; // Smoothing factor for historical data

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
        totalVehiclesSpawned = env.getVehiclePositions().size();

        for (RoadSegment road : env.getRoads().values()) {
            int currentCount = env.countVehiclesOnRoad(road.getId());
            double previousHistory = roadCongestionHistory.getOrDefault(road.getId(), (double) currentCount);

            // Moving average: History = (1-α)*History + α*Current
            double newHistory = (1.0 - ALPHA) * previousHistory + ALPHA * currentCount;
            roadCongestionHistory.put(road.getId(), newHistory);

            // Push to environment for other agents to use
            env.updateHistoricalCongestion(road.getId(), newHistory);
        }
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
