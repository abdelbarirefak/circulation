package com.traffic.model;

import java.util.Optional;

/**
 * Structured data representing the world as seen by a VehicleAgent.
 */
public class PerceptionData {
    private Optional<Obstacle> leadVehicle = Optional.empty();
    private Optional<Obstacle> leftVehicle = Optional.empty();
    private Optional<Obstacle> rightVehicle = Optional.empty();
    private Optional<Obstacle> leftBehind = Optional.empty();
    private Optional<Obstacle> rightBehind = Optional.empty();
    private Optional<LightInfo> trafficLight = Optional.empty();
    private boolean incidentAhead = false;
    private boolean emergencyBrake = false;

    public static class Obstacle {
        public String name;
        public double distance;
        public double relativeSpeed;
        public int lane;

        public Obstacle(String name, double distance, double relativeSpeed, int lane) {
            this.name = name;
            this.distance = distance;
            this.relativeSpeed = relativeSpeed;
            this.lane = lane;
        }
    }

    public static class LightInfo {
        public String name;
        public LightState state;
        public double distance;

        public LightInfo(String name, LightState state, double distance) {
            this.name = name;
            this.state = state;
            this.distance = distance;
        }
    }

    // Getters and Setters
    public Optional<Obstacle> getLeadVehicle() {
        return leadVehicle;
    }

    public void setLeadVehicle(Obstacle v) {
        this.leadVehicle = Optional.of(v);
    }

    public Optional<Obstacle> getLeftVehicle() {
        return leftVehicle;
    }

    public void setLeftVehicle(Obstacle v) {
        this.leftVehicle = Optional.of(v);
    }

    public Optional<Obstacle> getRightVehicle() {
        return rightVehicle;
    }

    public void setRightVehicle(Obstacle v) {
        this.rightVehicle = Optional.of(v);
    }

    public Optional<Obstacle> getLeftBehind() {
        return leftBehind;
    }

    public void setLeftBehind(Obstacle v) {
        this.leftBehind = Optional.of(v);
    }

    public Optional<Obstacle> getRightBehind() {
        return rightBehind;
    }

    public void setRightBehind(Obstacle v) {
        this.rightBehind = Optional.of(v);
    }

    public Optional<LightInfo> getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(LightInfo l) {
        this.trafficLight = Optional.of(l);
    }

    public boolean isIncidentAhead() {
        return incidentAhead;
    }

    public void setIncidentAhead(boolean incidentAhead) {
        this.incidentAhead = incidentAhead;
    }

    public boolean isEmergencyBrake() {
        return emergencyBrake;
    }

    public void setEmergencyBrake(boolean emergencyBrake) {
        this.emergencyBrake = emergencyBrake;
    }
}
