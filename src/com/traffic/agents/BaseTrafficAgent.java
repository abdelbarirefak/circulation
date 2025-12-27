package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import com.traffic.model.Position;
import com.traffic.environment.Environment;

public abstract class BaseTrafficAgent extends Agent {
    protected Position position;
    protected double speed;
    protected double direction; // in radians
    protected double perceptionRadius;

    @Override
    protected void setup() {
        initializeProperties();

        addBehaviour(new TickerBehaviour(this, 50) {
            @Override
            protected void onTick() {
                perceive();
                decide();
                // Synchronize state with environment
                Environment.getInstance().updateVehicleState(getLocalName(), position, null, speed, null, null);

                ACLMessage msg = receive();
                while (msg != null) {
                    handleMessage(msg);
                    msg = receive();
                }
                decide();
            }
        });
    }

    protected abstract void initializeProperties();

    protected abstract void perceive();

    protected abstract void handleMessage(ACLMessage msg);

    protected abstract void decide();

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }
}
