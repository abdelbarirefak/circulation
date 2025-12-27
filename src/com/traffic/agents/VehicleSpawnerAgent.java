package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import com.traffic.model.*;
import com.traffic.environment.Environment;

public class VehicleSpawnerAgent extends Agent {
    private int vehicleCount = 0;
    private String startRoadId = "R1";
    private double startX = 50.0;
    private double startY = 200.0;

    protected void setup() {
        System.out.println("VehicleSpawnerAgent started.");

        // Variable Ticker: period changes over time
        addBehaviour(new TickerBehaviour(this, 1000) {
            private long startTime = System.currentTimeMillis();

            @Override
            protected void onTick() {
                try {
                    // Update period based on rush hour cycle (e.g. 2 min period)
                    long elapsed = System.currentTimeMillis() - startTime;
                    double cycle = (Math.sin(elapsed / 60000.0 * Math.PI) + 1.0) / 2.0; // 0.0 to 1.0

                    // Period ranges from 1s (rush) to 5s (quiet)
                    int nextPeriod = (int) (5000 - (cycle * 4000));
                    reset(nextPeriod);

                    com.traffic.environment.Environment env = com.traffic.environment.Environment.getInstance();
                    int activeVehicles = env.getVehiclePositions().size();

                    if (activeVehicles >= 5) {
                        return; // Global limit enforced
                    }

                    vehicleCount++;
                    String[] profiles = { "CAUTIOUS", "NORMAL", "AGGRESSIVE" };
                    String profile = profiles[(int) (Math.random() * profiles.length)];

                    double randomOffsetX = (Math.random() - 0.5) * 10;
                    double randomOffsetY = (Math.random() - 0.5) * 5;

                    int lane = (int) (Math.random() * 2);

                    // Choose a random destination intersection
                    String destInter = null;
                    if (!env.getIntersections().isEmpty()) {
                        Object[] inters = env.getIntersections().keySet().toArray();
                        destInter = inters[(int) (Math.random() * inters.length)].toString();
                    }

                    // Decide agent type: 10% Bus, 5% Emergency, 85% Regular
                    String agentClass = "com.traffic.agents.VehicleAgent";
                    String namePrefix = "Auto_Veh_";
                    double rand = Math.random();
                    if (rand < 0.10) {
                        agentClass = "com.traffic.agents.PublicTransportAgent";
                        namePrefix = "Bus_";
                        profile = "NORMAL";
                    } else if (rand < 0.15) {
                        agentClass = "com.traffic.agents.EmergencyVehicleAgent";
                        profile = "AGGRESSIVE";
                        namePrefix = "Emerg_Veh_";
                    }

                    // Dynamically pick a source road
                    java.util.List<RoadSegment> sources = new java.util.ArrayList<>();
                    for (RoadSegment r : env.getRoads().values()) {
                        boolean isTarget = false;
                        for (Intersection inter : env.getIntersections().values()) {
                            if (inter.getOutgoingRoads().contains(r)) {
                                isTarget = true;
                                break;
                            }
                        }
                        if (!isTarget)
                            sources.add(r);
                    }

                    RoadSegment sourceRoad = sources.get((int) (Math.random() * sources.size()));
                    String startRoadId = sourceRoad.getId();
                    double startX = sourceRoad.getStart().getX();
                    double startY = sourceRoad.getStart().getY();

                    AgentContainer container = (AgentContainer) getContainerController();
                    AgentController vehicle = container.createNewAgent(
                            namePrefix + vehicleCount,
                            agentClass,
                            new Object[] { String.valueOf(startX + randomOffsetX),
                                    String.valueOf(startY + randomOffsetY), startRoadId, profile,
                                    String.valueOf(lane), destInter });
                    vehicle.start();

                    if (vehicleCount % 10 == 0) {
                        System.out.println("Spawner Peak Factor: " + String.format("%.2f", cycle) + " (Period: "
                                + nextPeriod + "ms)");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
