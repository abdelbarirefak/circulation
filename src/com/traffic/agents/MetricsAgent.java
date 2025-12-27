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
    private Map<String, Double> roadCongestionHistory = new HashMap<>();
    private final double ALPHA = 0.2;

    @Override
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
            double newHistory = (1.0 - ALPHA) * previousHistory + ALPHA * currentCount;
            roadCongestionHistory.put(road.getId(), newHistory);
            env.updateHistoricalCongestion(road.getId(), newHistory);
        }
    }

    private void reportMetrics() {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("--- METRICS [" + uptime + "s] Active: " + totalVehiclesSpawned);
        try (FileWriter writer = new FileWriter("metrics.csv", true)) {
            writer.write(uptime + "," + totalVehiclesSpawned + "\n");
        } catch (IOException e) {
        }
    }
}
