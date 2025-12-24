package com.traffic.model;

public class Position {
    private double x;
    private int lane;

    public Position(double x, int lane) {
        this.x = x;
        this.lane = lane;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    @Override
    public String toString() {
        return "Pos(x=" + String.format("%.1f", x) + ", lane=" + lane + ")";
    }
}
