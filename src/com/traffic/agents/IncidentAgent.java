package com.traffic.agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import com.traffic.environment.Environment;
import com.traffic.model.Position;

public class IncidentAgent extends Agent {
    private String roadId;
    private double x, y;
    private long duration;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 4) {
            roadId = args[0].toString();
            x = Double.parseDouble(args[1].toString());
            y = Double.parseDouble(args[2].toString());
            duration = Long.parseLong(args[3].toString());
        }

        System.out.println("INCIDENT DEPLOYED on " + roadId + " at (" + x + "," + y + ")");

        final Environment.Incident incident = new Environment.Incident(roadId, new Position(x, y), duration);
        Environment.getInstance().addIncident(incident);

        addBehaviour(new jade.core.behaviours.WakerBehaviour(this, duration) {
            @Override
            protected void onWake() {
                Environment.getInstance().removeIncident(incident);
                System.out.println("INCIDENT CLEARED on " + roadId);
                doDelete();
            }
        });
    }
}
