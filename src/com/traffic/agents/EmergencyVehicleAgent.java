package com.traffic.agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import com.traffic.environment.Environment;

public class EmergencyVehicleAgent extends VehicleAgent {

    @Override
    protected void initializeProperties() {
        super.initializeProperties();
        // Priority vehicles are faster and safer
        perceptionRadius *= 1.5;
        currentMaxSpeed *= 1.3;
    }

    @Override
    protected void decide() {
        super.decide();
        // Periodically request priority from traffic lights
        if (System.currentTimeMillis() % 5000 < 100) {
            requestPriority();
        }
        currentAction = "EMERGENCY RESPONSE";
    }

    private void requestPriority() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("PRIORITY_PASS");
        // Find nearest light
        String light = Environment.getInstance().getNearestLight(position, 300.0);
        if (light != null) {
            msg.addReceiver(new AID(light, AID.ISLOCALNAME));
            send(msg);
        }
    }
}
