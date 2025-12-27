package com.traffic.environment;

import com.traffic.model.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Environment {
    private static Environment instance;

    // Physical state
    private Map<String, Position> vehiclePositions = new ConcurrentHashMap<>();
    private Map<String, LightState> lightStates = new ConcurrentHashMap<>();
    private Map<String, Position> lightPositions = new ConcurrentHashMap<>();

    // Map structure
    private Map<String, RoadSegment> roads = new ConcurrentHashMap<>();
    private Map<String, Intersection> intersections = new ConcurrentHashMap<>();
    private List<Incident> activeIncidents = new ArrayList<>();

    public static class Incident {
        public String roadId;
        public Position position;
        public long duration;

        public Incident(String roadId, Position position, long duration) {
            this.roadId = roadId;
            this.position = position;
            this.duration = duration;
        }
    }

    public void addIncident(Incident incident) {
        activeIncidents.add(incident);
    }

    public void removeIncident(Incident incident) {
        activeIncidents.remove(incident);
    }

    public List<Incident> getActiveIncidents() {
        return activeIncidents;
    }

    private Environment() {
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    // Map Management
    public void addRoad(RoadSegment road) {
        roads.put(road.getId(), road);
    }

    public void addIntersection(Intersection inter) {
        intersections.put(inter.getId(), inter);
    }

    public Map<String, RoadSegment> getRoads() {
        return roads;
    }

    public Map<String, Intersection> getIntersections() {
        return intersections;
    }

    // Dynamic State Management
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

    // Spatial Queries
    public List<String> getNearbyAgents(Position myPos, double radius, String excludeName) {
        return vehiclePositions.entrySet().parallelStream()
                .filter(e -> !e.getKey().equals(excludeName))
                .filter(e -> e.getValue().distanceTo(myPos) <= radius)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public String getNearestLight(Position myPos, double radius) {
        String nearest = null;
        double minDist = radius;

        for (Map.Entry<String, Position> entry : lightPositions.entrySet()) {
            double dist = entry.getValue().distanceTo(myPos);
            if (dist < minDist) {
                minDist = dist;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

    public RoadSegment getRoadAt(Position pos) {
        RoadSegment nearest = null;
        double minDist = Double.MAX_VALUE;
        for (RoadSegment road : roads.values()) {
            Position mid = new Position((road.getStart().getX() + road.getEnd().getX()) / 2,
                    (road.getStart().getY() + road.getEnd().getY()) / 2);
            double dist = pos.distanceTo(mid);
            if (dist < minDist) {
                minDist = dist;
                nearest = road;
            }
        }
        return nearest;
    }

    public int countVehiclesOnRoad(String roadId) {
        RoadSegment road = roads.get(roadId);
        if (road == null)
            return 0;

        int count = 0;
        for (Position pos : vehiclePositions.values()) {
            double d = pos.distanceTo(road.getStart()) + pos.distanceTo(road.getEnd());
            if (Math.abs(d - road.getLength()) < 20.0) {
                count++;
            }
        }
        return count;
    }
}
