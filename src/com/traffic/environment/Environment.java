package com.traffic.environment;

import com.traffic.model.LightState;
import com.traffic.model.Position;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Environment {
    private static Environment instance;

    // Map of agent name -> position
    private Map<String, Position> vehiclePositions = new ConcurrentHashMap<>();
    // Map of light name -> state (position is fixed)
    private Map<String, LightState> lightStates = new ConcurrentHashMap<>();
    private Map<String, Position> lightPositions = new ConcurrentHashMap<>();

    private double roadLength = 1000.0;
    private int numLanes = 2;

    private Environment() {
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    public void updateVehiclePosition(String name, Position pos) {
        vehiclePositions.put(name, pos);
    }

    public void removeVehicle(String name) {
        vehiclePositions.remove(name);
    }

    public void updateLightState(String name, Position pos, LightState state) {
        lightPositions.put(name, pos);
        lightStates.put(name, state);
    }

    public Map<String, Position> getVehiclePositions() {
        return vehiclePositions;
    }

    public Map<String, LightState> getLightStates() {
        return lightStates;
    }

    public Map<String, Position> getLightPositions() {
        return lightPositions;
    }

    public double getRoadLength() {
        return roadLength;
    }

    public int getNumLanes() {
        return numLanes;
    }

    // Helper to find vehicle ahead in same lane
    public Double getVehicleAhead(String myName, Position myPos) {
        double minDistance = Double.MAX_VALUE;
        Double aheadX = null;

        for (Map.Entry<String, Position> entry : vehiclePositions.entrySet()) {
            if (entry.getKey().equals(myName))
                continue;

            Position otherPos = entry.getValue();
            if (otherPos.getLane() == myPos.getLane()) {
                double dist = otherPos.getX() - myPos.getX();
                if (dist > 0 && dist < minDistance) {
                    minDistance = dist;
                    aheadX = otherPos.getX();
                }
            }
        }
        return aheadX;
    }

    // Helper to find if there's a light ahead
    public String getLightAhead(Position myPos, double horizon) {
        for (Map.Entry<String, Position> entry : lightPositions.entrySet()) {
            Position lPos = entry.getValue();
            double dist = lPos.getX() - myPos.getX();
            if (dist > 0 && dist < horizon) {
                return entry.getKey();
            }
        }
        return null;
    }
}
