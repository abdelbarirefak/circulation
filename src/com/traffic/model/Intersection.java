package com.traffic.model;

import java.util.ArrayList;
import java.util.List;

public class Intersection {
    private String id;
    private Position position;
    private List<RoadSegment> incomingRoads = new ArrayList<>();
    private List<RoadSegment> outgoingRoads = new ArrayList<>();

    public Intersection(String id, Position position) {
        this.id = id;
        this.position = position;
    }

    public void addIncoming(RoadSegment road) {
        incomingRoads.add(road);
    }

    public void addOutgoing(RoadSegment road) {
        outgoingRoads.add(road);
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public List<RoadSegment> getIncomingRoads() {
        return incomingRoads;
    }

    public List<RoadSegment> getOutgoingRoads() {
        return outgoingRoads;
    }
}
