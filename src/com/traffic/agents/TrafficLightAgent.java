package com.traffic.agents;

import jade.lang.acl.ACLMessage;
import jade.core.AID;
import com.traffic.environment.Environment;
import com.traffic.model.*;

public class TrafficLightAgent extends BaseTrafficAgent {
    private LightState currentState = LightState.RED;
    private String targetRoadId;
    private long lastSwitchTime;
    private int currentGreenDuration = 10000;
    private final int YELLOW_DURATION = 3000;
    private final int MIN_GREEN = 5000;
    private final int MAX_GREEN = 40000;
    private double[][] qTable = new double[3][3];
    private final double LEARNING_RATE = 0.1;
    private final double DISCOUNT = 0.9;
    private int lastState = 0;
    private int lastAction = 1;
    private Intersection myIntersection;

    @Override
    protected void initializeProperties() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            position = new Position(Double.parseDouble(args[0].toString()), Double.parseDouble(args[1].toString()));
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

        int targetQueue = env.countVehiclesOnRoad(targetRoadId);
        int totalQueue = 0;
        if (myIntersection != null) {
            for (RoadSegment incoming : myIntersection.getIncomingRoads()) {
                totalQueue += env.countVehiclesOnRoad(incoming.getId());
            }
        }

        double reward = -totalQueue;
        if (lastState != -1) {
            double oldQ = qTable[lastState][lastAction];
            qTable[lastState][lastAction] = oldQ + LEARNING_RATE * (reward + DISCOUNT * 0 - oldQ);
        }

        int densityState = categorizeDensity(targetQueue);
        int action = selectAction(densityState);

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
            if (currentState != LightState.GREEN)
                switchTo(LightState.GREEN);
            lastSwitchTime = System.currentTimeMillis() + 8000;
        }
    }

    @Override
    protected void decide() {
        long elapsed = System.currentTimeMillis() - lastSwitchTime;
        switch (currentState) {
            case RED:
                if (elapsed > 10000)
                    switchTo(LightState.GREEN);
                break;
            case GREEN:
                if (elapsed > currentGreenDuration)
                    switchTo(LightState.YELLOW);
                break;
            case YELLOW:
                if (elapsed > YELLOW_DURATION)
                    switchTo(LightState.RED);
                break;
        }
        Environment.getInstance().updateLightState(getLocalName(), position, currentState);
    }

    private void switchTo(LightState next) {
        currentState = next;
        lastSwitchTime = System.currentTimeMillis();
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.setContent(currentState.toString());
        for (String agentName : Environment.getInstance().getVehiclePositions().keySet()) {
            inform.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }
        send(inform);
    }
}
