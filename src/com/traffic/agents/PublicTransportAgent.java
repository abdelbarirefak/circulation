package com.traffic.agents;

import jade.lang.acl.ACLMessage;
import com.traffic.model.*;

public class PublicTransportAgent extends VehicleAgent {

    @Override
    protected void decide() {
        super.decide();

        // 1. Bus Priority Logic at Traffic Lights
        if (perceptionData.getTrafficLight().isPresent()) {
            PerceptionData.LightInfo light = perceptionData.getTrafficLight().get();
            // Only request if light is RED and we are within range
            if (light.state == LightState.RED && light.distance < 150.0 && light.distance > 30.0) {
                // 30% chance to request priority to mimic "priority sensors"
                if (Math.random() < 0.3) {
                    sendPriorityRequest(light.name);
                }
            }
        }
    }

    private void sendPriorityRequest(String lightName) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("PRIORITY_PASS");
        msg.addReceiver(new jade.core.AID(lightName, jade.core.AID.ISLOCALNAME));
        send(msg);
        System.out.println(getLocalName() + " (Bus) requested priority from " + lightName);
    }
}
