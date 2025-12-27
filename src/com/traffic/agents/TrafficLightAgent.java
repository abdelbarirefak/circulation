package com.traffic.agents;

import jade.lang.acl.ACLMessage;
import com.traffic.environment.Environment;
import com.traffic.model.*;

public class TrafficLightAgent extends BaseTrafficAgent {
    private LightState currentState = LightState.RED;
    private String targetRoadId; // The road this light monitors
    private long lastSwitchTime;

    private int currentGreenDuration = 10000;
    private final int YELLOW_DURATION = 3000;
    private final int MIN_GREEN = 5000;
    private final int MAX_GREEN = 30000;

    @Override
    protected void initializeProperties() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            double x = Double.parseDouble(args[0].toString());
            double y = Double.parseDouble(args[1].toString());
            position = new Position(x, y);
            targetRoadId = args[2].toString();
        } else {
            position = new Position(0, 0);
            targetRoadId = "R1";
        }

        lastSwitchTime = System.currentTimeMillis();
        speed = 0;
        direction = 0;
        perceptionRadius = 0;
    }

    @Override
    protected void perceive() {
        // Adaptive logic: sense density on the target road
        Environment env = Environment.getInstance();
        int vehicleCount = env.countVehiclesOnRoad(targetRoadId);

        // Dynamic Green Calculation: 5s base + 3s per vehicle
        if (currentState == LightState.RED) {
            currentGreenDuration = Math.min(MAX_GREEN, MIN_GREEN + (vehicleCount * 3000));
        }
    }

    @Override
    protected void handleMessage(ACLMessage msg) {
        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().equals("PRIORITY_PASS")) {
            System.out.println(getLocalName() + " RECEIVED PRIORITY REQUEST. OVERRIDING CYCLE.");
            if (currentState != LightState.GREEN) {
                switchTo(LightState.GREEN);
            }
            // Reset timer and ensure long enough for emergency vehicle (e.g., 8s extra)
            lastSwitchTime = System.currentTimeMillis() + 8000;
        }
    }

    @Override
    protected void decide() {
        long elapsed = System.currentTimeMillis() - lastSwitchTime;

        switch (currentState) {
            case RED:
                // Red time could also be adaptive, but for now fixed 10s
                if (elapsed > 10000) {
                    switchTo(LightState.GREEN);
                }
                break;
            case GREEN:
                if (elapsed > currentGreenDuration) {
                    switchTo(LightState.YELLOW);
                }
                break;
            case YELLOW:
                if (elapsed > YELLOW_DURATION) {
                    switchTo(LightState.RED);
                }
                break;
        }

        Environment.getInstance().updateLightState(getLocalName(), position, currentState);
    }

    private void switchTo(LightState next) {
        currentState = next;
        lastSwitchTime = System.currentTimeMillis();
        System.out
                .println(getLocalName() + " switched to " + next + " (Green duration: " + currentGreenDuration + "ms)");

        // Broadcast state change to nearby vehicles
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.setContent(currentState.toString());
        // For simplicity, we broadcast to all, but agents filter by position
        for (String agentName : Environment.getInstance().getVehiclePositions().keySet()) {
            inform.addReceiver(new jade.core.AID(agentName, jade.core.AID.ISLOCALNAME));
        }
        send(inform);
    }
}
