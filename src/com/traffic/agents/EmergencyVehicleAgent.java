package com.traffic.agents;

import jade.lang.acl.ACLMessage;
import com.traffic.model.LightState;
import com.traffic.model.PerceptionData;
import com.traffic.environment.Environment;

public class EmergencyVehicleAgent extends VehicleAgent {
    private long lastPriorityRequestTime = 0;

    @Override
    protected void decide() {
        // High priority behavior: ignore some speed limits (handled by profile)
        super.decide();

        // Broadcast priority request to nearby traffic lights
        long now = System.currentTimeMillis();
        if (now - lastPriorityRequestTime > 2000) { // Every 2 seconds
            if (perceptionData != null && perceptionData.getTrafficLight().isPresent()) {
                PerceptionData.LightInfo light = perceptionData.getTrafficLight().get();
                if (light.distance < 150.0 && light.state != LightState.GREEN) {
                    sendPriorityRequest(light.name);
                    lastPriorityRequestTime = now;
                }
            }
        }
    }

    private void sendPriorityRequest(String lightName) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("PRIORITY_PASS");
        msg.addReceiver(new jade.core.AID(lightName, jade.core.AID.ISLOCALNAME));
        send(msg);
        System.out.println(getLocalName() + " SENT PRIORITY REQUEST TO " + lightName);
    }
}
