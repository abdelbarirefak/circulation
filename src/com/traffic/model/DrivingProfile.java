package com.traffic.model;

public enum DrivingProfile {
    CAUTIOUS(0.8, 1.5, 0.05, 0.2),
    NORMAL(1.0, 1.0, 0.1, 0.3),
    AGGRESSIVE(1.2, 0.7, 0.2, 0.5);

    private final double speedMultiplier;
    private final double safetyMultiplier;
    private final double accelRate;
    private final double brakeRate;

    DrivingProfile(double speedMult, double safetyMult, double accel, double brake) {
        this.speedMultiplier = speedMult;
        this.safetyMultiplier = safetyMult;
        this.accelRate = accel;
        this.brakeRate = brake;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getSafetyMultiplier() {
        return safetyMultiplier;
    }

    public double getAccelRate() {
        return accelRate;
    }

    public double getBrakeRate() {
        return brakeRate;
    }
}
