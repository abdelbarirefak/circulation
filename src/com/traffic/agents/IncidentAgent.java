package com.traffic.agents;

import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import com.traffic.environment.Environment;
import com.traffic.model.Position;

public class IncidentAgent extends BaseTrafficAgent {
    private String roadId;
    private long duration;

    @Override
    protected void initializeProperties() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            double x = Double.parseDouble(args[0].toString());
            double y = Double.parseDouble(args[1].toString());
            roadId = args[2].toString();
            duration = (args.length >= 4) ? Long.parseLong(args[3].toString()) : 60000;
            position = new Position(x, y);
        } else {
            position = new Position(0, 0);
            roadId = "R1";
            duration = 60000;
        }
        speed = 1.0; // Minimal speed for rendering
        direction = 0;
        perceptionRadius = 0;

        Environment.getInstance().addIncident(roadId, position, duration);

        addBehaviour(new WakerBehaviour(this, duration) {
            @Override
            protected void handleElapsedTimeout() {
                myAgent.doDelete();
            }
        });
    }

    @Override
    protected void perceive() {
    }

    @Override
    protected void handleMessage(ACLMessage msg) {
    }

    @Override
    protected void decide() {
    }

    @Override
    protected void takeDown() {
        Environment.getInstance().removeIncident(roadId, position);
    }
}
