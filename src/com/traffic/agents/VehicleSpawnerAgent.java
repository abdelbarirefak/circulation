package com.traffic.agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import com.traffic.environment.Environment;
import com.traffic.model.DrivingProfile;
import com.traffic.model.RoadSegment;
import java.util.ArrayList;

public class VehicleSpawnerAgent extends BaseTrafficAgent {
    private int spawnCount = 0;
    private long lastSpawnTime = 0;
    private float baseSpawnRate = 0.3f; // seconds

    @Override
    protected void initializeProperties() {
        position = new com.traffic.model.Position(0, 0);
        speed = 0;
        direction = 0;
        perceptionRadius = 0;
    }

    @Override
    protected void perceive() {
    }

    @Override
    protected void handleMessage(ACLMessage msg) {
    }

    @Override
    protected void decide() {
        long now = System.currentTimeMillis();
        float currentRate = baseSpawnRate * getRushHourMultiplier();

        if (now - lastSpawnTime > (1.0 / currentRate) * 1000 && spawnCount < 50) {
            spawnVehicle();
            lastSpawnTime = now;
        }
    }

    private void spawnVehicle() {
        Environment env = Environment.getInstance();
        if (env.getRoads().isEmpty())
            return;

        RoadSegment startRoad = (RoadSegment) env.getRoads().values()
                .toArray()[(int) (Math.random() * env.getRoads().size())];
        String vehicleName = "Vehicle-" + (++spawnCount);

        try {
            ContainerController cc = getContainerController();
            Object[] args = new Object[] {
                    startRoad.getStart().getX(),
                    startRoad.getStart().getY(),
                    startRoad.getId(),
                    DrivingProfile.values()[(int) (Math.random() * DrivingProfile.values().length)],
                    (int) (Math.random() * startRoad.getLanes())
            };

            AgentController ac = cc.createNewAgent(vehicleName, "com.traffic.agents.VehicleAgent", args);
            ac.start();
        } catch (Throwable e) {
            System.err.println("FAILED TO SPAWN VEHICLE: " + vehicleName);
            e.printStackTrace();
        }
    }

    private float getRushHourMultiplier() {
        long hour = (System.currentTimeMillis() / 10000) % 24; // 10s = 1 hour simulation time
        if ((hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 18))
            return 2.5f;
        return 1.0f;
    }
}
