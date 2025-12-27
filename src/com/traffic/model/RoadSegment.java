package com.traffic.model;

public class RoadSegment {
    private String id;
    private Position start;
    private Position end;
    private int lanes;
    private double speedLimit;
    private boolean isOneWay;

    public RoadSegment(String id, Position start, Position end, int lanes, double speedLimit, boolean isOneWay) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.lanes = lanes;
        this.speedLimit = speedLimit;
        this.isOneWay = isOneWay;
    }

    public String getId() {
        return id;
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }

    public int getLanes() {
        return lanes;
    }

    public double getSpeedLimit() {
        return speedLimit;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public static final double LANE_WIDTH = 25.0;

    public double getLaneOffset(int laneIndex) {
        // Calculate offset from central axis.
        // For 2 lanes: lane 0 is -12.5, lane 1 is +12.5
        double totalWidth = lanes * LANE_WIDTH;
        return (laneIndex * LANE_WIDTH) - (totalWidth / 2.0) + (LANE_WIDTH / 2.0);
    }

    public double getLength() {
        return start.distanceTo(end);
    }

    public double getAngle() {
        return Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());
    }
}
