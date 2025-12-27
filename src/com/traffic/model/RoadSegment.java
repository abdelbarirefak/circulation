package com.traffic.model;

public class RoadSegment {
    private String id;
    private Position start;
    private Position end;
    private int lanes;
    private double speedLimit;
    private boolean isOneWay;

    private Position controlPoint; // For Quadratic Bezier curves
    private boolean isCurved = false;
    private boolean isYieldTarget = false; // For roundabout yielding

    public RoadSegment(String id, Position start, Position end, int lanes, double speedLimit, boolean isOneWay) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.lanes = lanes;
        this.speedLimit = speedLimit;
        this.isOneWay = isOneWay;
    }

    public RoadSegment(String id, Position start, Position end, Position controlPoint, int lanes, double speedLimit,
            boolean isOneWay) {
        this(id, start, end, lanes, speedLimit, isOneWay);
        this.controlPoint = controlPoint;
        this.isCurved = (controlPoint != null);
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
        if (!isCurved)
            return start.distanceTo(end);
        // Approximation for Bezier length (midpoint sampling)
        double length = 0;
        Position prev = start;
        for (int i = 1; i <= 10; i++) {
            Position current = getPointAt(i / 10.0);
            length += prev.distanceTo(current);
            prev = current;
        }
        return length;
    }

    public Position getPointAt(double t) {
        if (!isCurved) {
            return new Position(
                    start.getX() + (end.getX() - start.getX()) * t,
                    start.getY() + (end.getY() - start.getY()) * t);
        }
        // B(t) = (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
        double x = Math.pow(1 - t, 2) * start.getX() + 2 * (1 - t) * t * controlPoint.getX()
                + Math.pow(t, 2) * end.getX();
        double y = Math.pow(1 - t, 2) * start.getY() + 2 * (1 - t) * t * controlPoint.getY()
                + Math.pow(t, 2) * end.getY();
        return new Position(x, y);
    }

    public double getAngleAt(double t) {
        if (!isCurved)
            return getAngle();
        // Derivative B'(t) = 2(1-t)(P1-P0) + 2t(P2-P1)
        double dx = 2 * (1 - t) * (controlPoint.getX() - start.getX()) + 2 * t * (end.getX() - controlPoint.getX());
        double dy = 2 * (1 - t) * (controlPoint.getY() - start.getY()) + 2 * t * (end.getY() - controlPoint.getY());
        return Math.atan2(dy, dx);
    }

    public double getAngle() {
        return Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());
    }

    public boolean isCurved() {
        return isCurved;
    }

    public Position getControlPoint() {
        return controlPoint;
    }

    public boolean isYieldTarget() {
        return isYieldTarget;
    }

    public void setYieldTarget(boolean yieldTarget) {
        isYieldTarget = yieldTarget;
    }
}
