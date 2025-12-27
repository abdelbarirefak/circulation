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
    private final int MAX_GREEN = 40000;

    // RL Parameters (Simplified Q-Learning)
    private double[][] qTable = new double[3][3]; // States: [DensityLow, Med, High] x Actions: [Shorten, Keep, Extend]
    private final double LEARNING_RATE = 0.1;
    private final double DISCOUNT = 0.9;
    private int lastState = 0;
    private int lastAction = 1;

    private Intersection myIntersection;

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
        Environment env = Environment.getInstance();
        if (myIntersection == null)
            findMyIntersection();

        // 1. Calculate Reward (Negative total waiting time/queue length)
        int totalQueue = 0;
        int targetQueue = env.countVehiclesOnRoad(targetRoadId);
        for (RoadSegment incoming : myIntersection.getIncomingRoads()) {
            totalQueue += env.countVehiclesOnRoad(incoming.getId());
        }
        double reward = -totalQueue;

        // 2. Perform RL Update (on transition from GREEN back to RED)
        // For simplicity, we update based on how the previous green duration worked
        if (lastState != -1) {
            double oldQ = qTable[lastState][lastAction];
            qTable[lastState][lastAction] = oldQ + LEARNING_RATE * (reward + DISCOUNT * 0 - oldQ);
        }

        // 3. Decide new Action based on current density state
        int densityState = categorizeDensity(targetQueue);
        int action = selectAction(densityState); // 0: -3s, 1: 0, 2: +3s

        if (action == 0)
            currentGreenDuration = Math.max(MIN_GREEN, currentGreenDuration - 3000);
        else if (action == 2)
            currentGreenDuration = Math.min(MAX_GREEN, currentGreenDuration + 3000);

        lastState = densityState;
        lastAction = action;
    }

    private void findMyIntersection() {
        Environment env = Environment.getInstance();
        for (Intersection inter : env.getIntersections().values()) {
            for (RoadSegment incoming : inter.getIncomingRoads()) {
                if (incoming.getId().equals(targetRoadId)) {
                    myIntersection = inter;
                    return;
                }
            }
        }
    }

    private int categorizeDensity(int count) {
        if (count < 3)
            return 0;
        if (count < 8)
            return 1;
        return 2;
    }

    private int selectAction(int state) {
        // Epsilon-Greedy
        if (Math.random() < 0.1)
            return (int) (Math.random() * 3);

        int best = 1;
        for (int a = 0; a < 3; a++) {
            if (qTable[state][a] > qTable[state][best])
                best = a;
        }
        return best;
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
