package com.traffic.model;

import java.util.ArrayList;
import java.util.List;

public class Roundabout {
    private String id;
    private Position center;
    private double radius;
    private List<String> incomingRoadIds;
    private List<String> outgoingRoadIds;

    public Roundabout(String id, Position center, double radius) {
        this.id = id;
        this.center = center;
        this.radius = radius;
        this.incomingRoadIds = new ArrayList<>();
        this.outgoingRoadIds = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Position getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public void addIncoming(String roadId) {
        incomingRoadIds.add(roadId);
    }

    public void addOutgoing(String roadId) {
        outgoingRoadIds.add(roadId);
    }

    public List<String> getIncomingRoadIds() {
        return incomingRoadIds;
    }

    public List<String> getOutgoingRoadIds() {
        return outgoingRoadIds;
    }
}
