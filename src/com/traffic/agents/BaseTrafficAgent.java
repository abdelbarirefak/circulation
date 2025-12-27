package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import com.traffic.model.Position;

public abstract class BaseTrafficAgent extends Agent {
    protected Position position;
    protected double speed;
    protected double direction; // in radians
    protected double perceptionRadius;

    @Override
    protected void setup() {
        initializeProperties();

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                perceive();
                ACLMessage msg = receive();
                while (msg != null) {
                    handleMessage(msg);
                    msg = receive();
                }
                decide();
                block(50);
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
