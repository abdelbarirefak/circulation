package com.traffic.model;

public class Position {
    private double x;
    private double y;
    private int lane;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
        this.lane = 0;
    }

    public Position(double x, double y, int lane) {
        this.x = x;
        this.y = y;
        this.lane = lane;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public double distanceTo(Position other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
    }

    @Override
    public String toString() {
        return "(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ")";
    }
}
