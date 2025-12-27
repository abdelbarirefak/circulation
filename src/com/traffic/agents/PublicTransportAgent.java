package com.traffic.agents;

import jade.lang.acl.ACLMessage;
import com.traffic.environment.Environment;
import com.traffic.model.RoadSegment;

public class PublicTransportAgent extends VehicleAgent {
    private int passengers;
    private long lastStopDeparture;
    private boolean isAtStop;

    @Override
    protected void initializeProperties() {
        super.initializeProperties();
        passengers = (int) (Math.random() * 40 + 10);
        lastStopDeparture = System.currentTimeMillis();
        // Buses are slower and more predictable
        currentMaxSpeed *= 0.8;
    }

    @Override
    protected void decide() {
        if (isAtStop) {
            if (System.currentTimeMillis() - lastStopDeparture > 10000) {
                isAtStop = false;
                lastStopDeparture = System.currentTimeMillis();
            } else {
                speed = 0;
                currentAction = "Loading Passengers (" + passengers + ")";
                Environment.getInstance().updateVehicleState(getLocalName(), position, currentRoadId, 0, currentAction,
                        null);
                return;
            }
        }

        // Logic to detect a virtual stop (e.g. every 500 units of progress or specific
        // landmarks)
        if (progress > 500 && Math.random() < 0.01) {
            isAtStop = true;
            progress = Math.floor(progress); // "Snap" to stop
        }

        super.decide();
        if (!isAtStop) {
            currentAction = "Transporting " + passengers + " Citizens";
        }
    }

    @Override
    protected void handleMessage(ACLMessage msg) {
        super.handleMessage(msg);
    }
}
