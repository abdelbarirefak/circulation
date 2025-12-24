package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import com.traffic.environment.Environment;
import com.traffic.model.LightState;
import com.traffic.model.Position;

public class TrafficLightAgent extends Agent {
    private LightState currentState = LightState.RED;
    private Position position;
    private int greenDuration = 10000; // 10s
    private int yellowDuration = 3000; // 3s
    private int redDuration = 10000; // 10s

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            double x = Double.parseDouble(args[0].toString());
            int lane = Integer.parseInt(args[1].toString());
            position = new Position(x, lane);
        } else {
            position = new Position(500.0, 0); // Default
        }

        System.out.println("TrafficLightAgent " + getLocalName() + " started at " + position);

        addBehaviour(new TickerBehaviour(this, 1000) {
            private long lastSwitchTime = System.currentTimeMillis();

            @Override
            protected void onTick() {
                long now = System.currentTimeMillis();
                long elapsed = now - lastSwitchTime;

                switch (currentState) {
                    case GREEN:
                        if (elapsed > greenDuration) {
                            currentState = LightState.YELLOW;
                            lastSwitchTime = now;
                        }
                        break;
                    case YELLOW:
                        if (elapsed > yellowDuration) {
                            currentState = LightState.RED;
                            lastSwitchTime = now;
                        }
                        break;
                    case RED:
                        if (elapsed > redDuration) {
                            currentState = LightState.GREEN;
                            lastSwitchTime = now;
                        }
                        break;
                }

                // Update environment
                Environment.getInstance().updateLightState(getLocalName(), position, currentState);
            }
        });
    }
}
