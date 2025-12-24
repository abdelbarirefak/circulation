package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import com.traffic.environment.Environment;
import com.traffic.model.LightState;
import com.traffic.model.Position;

public class CarAgent extends Agent {
    private Position position;
    private double speed = 0.0;
    private double maxSpeed = 5.0; // units per tick
    private double acceleration = 0.2;
    private double deceleration = 0.5;
    private double safetyDistance = 40.0;
    private double stopDistance = 10.0;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            double x = Double.parseDouble(args[0].toString());
            int lane = Integer.parseInt(args[1].toString());
            position = new Position(x, lane);
        } else {
            position = new Position(0.0, (int) (Math.random() * 2));
        }

        System.out.println("CarAgent " + getLocalName() + " started at " + position);

        addBehaviour(new TickerBehaviour(this, 100) { // 10 updates per second
            @Override
            protected void onTick() {
                drive();
            }
        });
    }

    private void drive() {
        Environment env = Environment.getInstance();
        boolean mustStop = false;

        // 1. Check for traffic lights ahead
        String lightName = env.getLightAhead(position, 100.0);
        if (lightName != null) {
            LightState state = env.getLightStates().get(lightName);
            Position lightPos = env.getLightPositions().get(lightName);
            double distToLight = lightPos.getX() - position.getX();

            if ((state == LightState.RED || state == LightState.YELLOW) && distToLight < 80.0) {
                mustStop = true;
                // Adjust speed to stop smoothly at the light
                if (distToLight > stopDistance) {
                    speed = Math.max(0, speed - deceleration);
                } else {
                    speed = 0;
                }
            }
        }

        // 2. Check for vehicles ahead
        Double aheadX = env.getVehicleAhead(getLocalName(), position);
        if (aheadX != null) {
            double dist = aheadX - position.getX();
            if (dist < safetyDistance) {
                mustStop = true;
                if (dist > stopDistance) {
                    speed = Math.max(0, speed - deceleration * 1.5);
                } else {
                    speed = 0;
                }
            }
        }

        // 3. Accelerate if clear
        if (!mustStop) {
            speed = Math.min(maxSpeed, speed + acceleration);
        }

        // 4. Update position
        position.setX(position.getX() + speed);

        // Loop road for continuous simulation
        if (position.getX() > env.getRoadLength()) {
            position.setX(0);
        }

        env.updateVehiclePosition(getLocalName(), position);
    }

    protected void takeDown() {
        Environment.getInstance().removeVehicle(getLocalName());
        System.out.println("CarAgent " + getLocalName() + " terminating.");
    }
}
