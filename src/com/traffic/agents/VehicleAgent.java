package com.traffic.agents;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.core.Agent;
import com.traffic.environment.Environment;
import com.traffic.model.*;
import com.traffic.logic.Pathfinder;

public class VehicleAgent extends BaseTrafficAgent {
    private String currentRoadId;
    private int lane;
    private double progress; // Distance along current road
    private DrivingProfile profile;
    private String destinationInterId;
    private List<String> plannedPath;

    private double currentMaxSpeed;
    private double currentSafetyRadius;
    protected PerceptionData perceptionData;
    private long lastRerouteTime = 0;

    @Override
    protected void initializeProperties() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            double x = Double.parseDouble(args[0].toString());
            double y = Double.parseDouble(args[1].toString());
            currentRoadId = args[2].toString();
            position = new Position(x, y);

            if (args.length >= 4) {
                profile = DrivingProfile.valueOf(args[3].toString().toUpperCase());
            } else {
                profile = DrivingProfile.NORMAL;
            }

            if (args.length >= 5) {
                lane = Integer.parseInt(args[4].toString());
                position.setLane(lane);
            }
            if (args.length >= 6 && args[5] != null) {
                destinationInterId = args[5].toString();
            }
        } else {
            position = new Position(0, 0);
            profile = DrivingProfile.NORMAL;
            currentRoadId = "R1";
            lane = 0;
        }

        speed = 0.0;
        perceptionRadius = 150.0 * profile.getSafetyMultiplier();
        currentSafetyRadius = 60.0 * profile.getSafetyMultiplier();
        perceptionData = new PerceptionData();

        // Compute path if destination is set
        Environment env = Environment.getInstance();
        if (destinationInterId != null) {
            Intersection startInter = findNextIntersectionForRoad(env, currentRoadId);
            if (startInter != null) {
                plannedPath = Pathfinder.findPath(env.getIntersections(), env.getRoads(),
                        startInter.getId(), destinationInterId);
            }
        }
        if (plannedPath == null) {
            plannedPath = new ArrayList<>();
        }
    }

    @Override
    protected void perceive() {
        Environment env = Environment.getInstance();
        RoadSegment road = env.getRoads().get(currentRoadId);
        if (road == null)
            return;

        direction = road.getAngle();
        currentMaxSpeed = road.getSpeedLimit() * profile.getSpeedMultiplier();

        // Reset perception
        perceptionData = new PerceptionData();

        // 1. Detect other vehicles in vision cone
        List<String> nearby = env.getNearbyAgents(position, perceptionRadius, getLocalName());
        for (String otherName : nearby) {
            Position otherPos = env.getVehiclePositions().get(otherName);
            double dist = position.distanceTo(otherPos);
            if (dist < 5.0)
                continue;

            double angleToOther = Math.atan2(otherPos.getY() - position.getY(), otherPos.getX() - position.getX());
            double angleDiff = Math.abs(angleToOther - direction);
            if (angleDiff > Math.PI)
                angleDiff = 2 * Math.PI - angleDiff;

            // Vision cone check (approx 45 degrees)
            if (angleDiff < Math.PI / 4) {
                PerceptionData.Obstacle obs = new PerceptionData.Obstacle(otherName, dist, 0, otherPos.getLane());

                if (otherPos.getLane() == lane) {
                    if (!perceptionData.getLeadVehicle().isPresent()
                            || dist < perceptionData.getLeadVehicle().get().distance) {
                        perceptionData.setLeadVehicle(obs);
                    }
                } else if (otherPos.getLane() == lane - 1) {
                    if (!perceptionData.getLeftVehicle().isPresent()
                            || dist < perceptionData.getLeftVehicle().get().distance) {
                        perceptionData.setLeftVehicle(obs);
                    }
                } else if (otherPos.getLane() == lane + 1) {
                    if (!perceptionData.getRightVehicle().isPresent()
                            || dist < perceptionData.getRightVehicle().get().distance) {
                        perceptionData.setRightVehicle(obs);
                    }
                }
            }
        }

        // 2. Detect Traffic Lights
        String lightName = env.getNearestLight(position, perceptionRadius);
        if (lightName != null) {
            LightState state = env.getLightStates().get(lightName);
            Position lightPos = env.getLightPositions().get(lightName);
            double dist = position.distanceTo(lightPos);

            double angleToLight = Math.atan2(lightPos.getY() - position.getY(), lightPos.getX() - position.getX());
            double angleDiff = Math.abs(angleToLight - direction);
            if (angleDiff > Math.PI)
                angleDiff = 2 * Math.PI - angleDiff;

            if (angleDiff < Math.PI / 6 && dist < 80.0) {
                perceptionData.setTrafficLight(new PerceptionData.LightInfo(lightName, state, dist));
            }
        }

        // 3. Detect Incidents
        for (Environment.Incident inc : env.getActiveIncidents()) {
            if (inc.roadId.equals(currentRoadId)) {
                double dist = position.distanceTo(inc.position);
                if (dist < perceptionRadius && dist > 5.0) {
                    // Check if ahead using angle
                    double angleToInc = Math.atan2(inc.position.getY() - position.getY(),
                            inc.position.getX() - position.getX());
                    double angleDiff = Math.abs(angleToInc - direction);
                    if (angleDiff > Math.PI)
                        angleDiff = 2 * Math.PI - angleDiff;

                    if (angleDiff < Math.PI / 4) {
                        // HAZARD WARNING: Broadcast to 500m radius
                        if (dist < 100.0) {
                            speed = Math.max(2.0, speed * 0.7);
                            // Broadcast hazard warning once every 2s
                            if (System.currentTimeMillis() % 2000 < 100) {
                                broadcastHazard(500.0);
                            }
                        }
                    }
                }
            }
        }
    }

    private void broadcastHazard(double radius) {
        broadcastV2V(ACLMessage.INFORM, "HAZARD_AHEAD", false, null);
    }

    @Override
    protected void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        if (msg.getPerformative() == ACLMessage.INFORM) {
            try {
                if (content.equals("RED") || content.equals("YELLOW") || content.equals("GREEN")) {
                    LightState state = LightState.valueOf(content);
                    String lightName = msg.getSender().getLocalName();
                    // If we already detected this light spatially, update its state from the
                    // message
                    if (perceptionData.getTrafficLight().isPresent()
                            && perceptionData.getTrafficLight().get().name.equals(lightName)) {
                        perceptionData.getTrafficLight().get().state = state;
                    }
                } else if (content.startsWith("BRAKING")) {
                    // Reactive braking if someone nearby is slamming brakes
                    double sourceX = Double.parseDouble(msg.getUserDefinedParameter("x"));
                    double sourceY = Double.parseDouble(msg.getUserDefinedParameter("y"));
                    Position sourcePos = new Position(sourceX, sourceY);

                    // Only care if they are reasonably close (V2V warning)
                    double dist = position.distanceTo(sourcePos);
                    if (dist < 100.0) {
                        // Anti-collision reaction: slow down immediately
                        speed = Math.max(0, speed - profile.getBrakeRate() * 1.5);
                    }
                } else if (content.startsWith("HAZARD_AHEAD")) {
                    // Proactive rerouting before seeing the incident
                    System.out.println(getLocalName() + " received HAZARD warning. Proactively rerouting...");
                    triggerReroute();
                } else if (content.startsWith("CACC_DATA")
                        && msg.getUserDefinedParameter("lane").equals(String.valueOf(lane))) {
                    // CACC: Adjust speed based on lead vehicle's current acceleration/braking
                    double leadAccel = Double.parseDouble(msg.getUserDefinedParameter("accel"));
                    speed = Math.max(0, speed + leadAccel * 0.8); // Follow lead's intent
                } else if (content.startsWith("CONGESTION")) {
                    // Trigger a reroute check if we are on a congested road
                    if (Math.random() < 0.3) {
                        triggerReroute();
                    }
                }
            } catch (Exception e) {
            }
        } else if (msg.getPerformative() == ACLMessage.REQUEST && content.equals("LANE_COOPERATE")) {
            // Be nice and slow down slightly to let someone in
            speed = Math.max(speed * 0.7, 1.0);
        }
    }

    private void broadcastV2V(int performative, String content, boolean includePos, Map<String, String> params) {
        Environment env = Environment.getInstance();
        double radius = content.startsWith("HAZARD") ? 500.0 : 200.0;
        List<String> nearby = env.getNearbyAgents(position, radius, getLocalName());
        if (nearby.isEmpty())
            return;

        ACLMessage msg = new ACLMessage(performative);
        msg.setContent(content);
        if (includePos) {
            msg.addUserDefinedParameter("x", String.valueOf(position.getX()));
            msg.addUserDefinedParameter("y", String.valueOf(position.getY()));
        }
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                msg.addUserDefinedParameter(entry.getKey(), entry.getValue());
            }
        }

        for (String name : nearby) {
            msg.addReceiver(new jade.core.AID(name, jade.core.AID.ISLOCALNAME));
        }
        send(msg);
    }

    @Override
    protected void decide() {
        Environment env = Environment.getInstance();
        RoadSegment road = env.getRoads().get(currentRoadId);
        if (road == null)
            return;

        // Check if we reached the intersection
        if (position.distanceTo(road.getEnd()) < 10.0) {
            handleIntersectionEntry(env, road);
        }

        boolean shouldBrake = false;
        double prevSpeed = speed;
        double accel = 0;

        // 1. Check Lead Vehicle (IDM-lite: Proportional Braking)
        if (perceptionData.getLeadVehicle().isPresent()) {
            PerceptionData.Obstacle lead = perceptionData.getLeadVehicle().get();
            double leadDist = lead.distance;

            // Dynamic Safety Distance: 1.5s gap + 20px constant buffer
            double safeGap = (speed * 1.5) + 20.0;

            if (leadDist < safeGap) {
                shouldBrake = true;
                // Proportional Deceleration: the closer we are, the harder we brake
                double gapRatio = (safeGap - leadDist) / safeGap;
                accel = -profile.getBrakeRate() * (0.5 + gapRatio);

                // Opportunistic Lane Changing if stuck
                if (leadDist < 30.0)
                    tryLaneChange(road);
            }
        }

        // 2. Check Traffic Light
        if (perceptionData.getTrafficLight().isPresent()) {
            PerceptionData.LightInfo light = perceptionData.getTrafficLight().get();
            if ((light.state == LightState.RED || light.state == LightState.YELLOW) && light.distance > 10.0) {
                shouldBrake = true;
            }
        }

        // 3. Acceleration / Braking Application
        if (shouldBrake) {
            // Apply calculated deceleration (or default if not set by IDM)
            if (accel == 0)
                accel = -profile.getBrakeRate();
            speed = Math.max(0, speed + accel);

            if (prevSpeed - speed > 0.5) {
                broadcastV2V(ACLMessage.INFORM, "BRAKING", true, null);
            }
        } else {
            accel = profile.getAccelRate();
            speed = Math.min(currentMaxSpeed, speed + accel);
        }

        // CACC Broadcast: Share accel with vehicle behind in same lane
        if (Math.abs(accel) > 0.01) {
            Map<String, String> caccParams = new HashMap<>();
            caccParams.put("accel", String.valueOf(accel));
            caccParams.put("lane", String.valueOf(lane));
            broadcastV2V(ACLMessage.INFORM, "CACC_DATA", false, caccParams);
        }

        // 4. Movement (Progress-based for curves)
        progress += speed;

        if (road != null) {
            updatePhysicalPosition(road);

            // Check for road completion
            if (progress >= road.getLength()) {
                handleRoadCompletion(road);
            }
        }

        env.updateVehiclePosition(getLocalName(), position, currentRoadId, accel);
    }

    private void updatePhysicalPosition(RoadSegment road) {
        double t = Math.min(1.0, progress / road.getLength());
        Position basePos = road.getPointAt(t);
        double roadAngle = road.getAngleAt(t);
        double perpAngle = roadAngle + Math.PI / 2.0;
        double offset = road.getLaneOffset(lane);

        position.setX(basePos.getX() + Math.cos(perpAngle) * offset);
        position.setY(basePos.getY() + Math.sin(perpAngle) * offset);
        direction = roadAngle;
    }

    private void handleRoadCompletion(RoadSegment road) {
        Environment env = Environment.getInstance();
        handleIntersectionEntry(env, road); // Existing logic to pick next road
        progress = 0; // Reset progress for the new road
    }

    private void handleIntersectionEntry(Environment env, RoadSegment currentRoad) {
        if (plannedPath != null && !plannedPath.isEmpty()) {
            // Take the next road in path
            String nextRoadId = plannedPath.remove(0);
            RoadSegment nextRoad = env.getRoads().get(nextRoadId);
            if (nextRoad != null) {
                currentRoadId = nextRoadId;
                position.setX(nextRoad.getStart().getX());
                position.setY(nextRoad.getStart().getY());
                direction = nextRoad.getAngle();
                applyLaneOffset(nextRoad);
                return;
            }
        }

        // Default: try to find any outgoing road if no path is planned
        Intersection inter = findNextIntersectionForRoad(env, currentRoadId);
        if (inter != null && !inter.getOutgoingRoads().isEmpty()) {
            RoadSegment nextRoad = inter.getOutgoingRoads().get(0);
            currentRoadId = nextRoad.getId();
            position.setX(nextRoad.getStart().getX());
            position.setY(nextRoad.getStart().getY());
            direction = nextRoad.getAngle();
            applyLaneOffset(nextRoad);
        } else {
            // No outgoing road, simulation ends for this vehicle
            env.removeVehicle(getLocalName());
            doDelete();
        }
    }

    private void applyLaneOffset(RoadSegment road) {
        if (road == null)
            return;
        double offset = road.getLaneOffset(lane);
        double perpAngle = road.getAngle() + Math.PI / 2.0;
        position.setX(position.getX() + Math.cos(perpAngle) * offset);
        position.setY(position.getY() + Math.sin(perpAngle) * offset);
    }

    private Intersection findNextIntersectionForRoad(Environment env, String roadId) {
        RoadSegment road = env.getRoads().get(roadId);
        if (road == null)
            return null;
        for (Intersection inter : env.getIntersections().values()) {
            if (inter.getIncomingRoads().contains(road)) {
                return inter;
            }
        }
        return null;
    }

    private void tryLaneChange(RoadSegment road) {
        if (road.getLanes() <= 1)
            return;

        // Check Left
        if (lane > 0 && isLaneChangeSafe(perceptionData.getLeftVehicle(), perceptionData.getLeftBehind())) {
            lane--;
            position.setLane(lane);
            System.out.println(getLocalName() + " changing to left lane.");
        }
        // Check Right
        else if (lane < road.getLanes() - 1
                && isLaneChangeSafe(perceptionData.getRightVehicle(), perceptionData.getRightBehind())) {
            lane++;
            position.setLane(lane);
            System.out.println(getLocalName() + " changing to right lane.");
        }
    }

    private boolean isLaneChangeSafe(Optional<PerceptionData.Obstacle> front,
            Optional<PerceptionData.Obstacle> behind) {
        double safeGap = (speed * 1.5) + 20.0;

        // Check front gap in target lane
        if (front.isPresent() && front.get().distance < safeGap)
            return false;

        // Check behind gap in target lane (avoid cutting someone off)
        if (behind.isPresent() && behind.get().distance < 30.0)
            return false;

        return true;
    }

    private void triggerReroute() {
        if (System.currentTimeMillis() - lastRerouteTime < 5000)
            return; // Once per 5s

        Environment env = Environment.getInstance();

        // Metrics-Driven heuristic: Only reroute if current road is worse than average
        double currentHist = env.getHistoricalCongestion(currentRoadId);
        if (currentHist < 2.0)
            return; // Road is fine, don't fluctuate

        Intersection nextInter = findNextIntersectionForRoad(env, currentRoadId);
        if (nextInter != null && destinationInterId != null) {
            plannedPath = Pathfinder.findPath(env.getIntersections(), env.getRoads(),
                    nextInter.getId(), destinationInterId);
            lastRerouteTime = System.currentTimeMillis();
            System.out.println(getLocalName() + " rerouted based on metrics. Current congestion rank: " + currentHist);
        }
    }

    private void handleRoadTransition(Environment env, RoadSegment road) {
        handleIntersectionEntry(env, road);
    }

    @Override
    protected void takeDown() {
        Environment.getInstance().removeVehicle(getLocalName());
    }
}
