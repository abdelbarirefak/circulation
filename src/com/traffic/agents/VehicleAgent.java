package com.traffic.agents;

import jade.lang.acl.ACLMessage;
import jade.core.AID;
import com.traffic.environment.Environment;
import com.traffic.model.*;
import com.traffic.logic.Pathfinder;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

public class VehicleAgent extends BaseTrafficAgent {
    private String currentRoadId;
    private int lane;
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
                        // Mark as obstacle or handle directly
                        if (dist < 100.0) {
                            speed = Math.max(2.0, speed * 0.7); // Congestion effect
                        }
                    }
                }
            }
        }
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
                } else if (content.startsWith("CONGESTION")) {
                    // Trigger a reroute check if we are on a congested road
                    if (Math.random() < 0.3) { // 30% chance to react and change path
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

    private void broadcastV2V(int performative, String content, boolean includePos) {
        Environment env = Environment.getInstance();
        List<String> nearby = env.getNearbyAgents(position, 200.0, getLocalName());
        if (nearby.isEmpty())
            return;

        ACLMessage msg = new ACLMessage(performative);
        msg.setContent(content);
        if (includePos) {
            msg.addUserDefinedParameter("x", String.valueOf(position.getX()));
            msg.addUserDefinedParameter("y", String.valueOf(position.getY()));
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

        // 1. Check Lead Vehicle (ACC Logics)
        if (perceptionData.getLeadVehicle().isPresent()) {
            double dist = perceptionData.getLeadVehicle().get().distance;
            if (dist < currentSafetyRadius) {
                shouldBrake = true;
                // Opportunistic Lane Changing
                tryLaneChange(road);

                // Rerouting logic: if stuck, try finding a better path
                if (speed < 0.5 && Math.random() < 0.05) {
                    triggerReroute();
                }
            }
        }

        // 2. Check Traffic Light
        if (perceptionData.getTrafficLight().isPresent()) {
            PerceptionData.LightInfo light = perceptionData.getTrafficLight().get();
            if ((light.state == LightState.RED || light.state == LightState.YELLOW) && light.distance > 10.0) {
                shouldBrake = true;
            }
        }

        // 3. Acceleration / Braking
        if (shouldBrake) {
            speed = Math.max(0, speed - profile.getBrakeRate());
            // Sudden braking alert if we lose significant speed
            if (prevSpeed - speed > 1.0) {
                broadcastV2V(ACLMessage.INFORM, "BRAKING", true);
            }
        } else {
            speed = Math.min(currentMaxSpeed, speed + profile.getAccelRate());
        }

        // 4. Update Position with Lane Offset
        moveWithOffset(road);

        env.updateVehiclePosition(getLocalName(), position);
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

        // Request cooperation from neighbors
        broadcastV2V(ACLMessage.REQUEST, "LANE_COOPERATE", false);

        // Try left then right
        if (lane > 0 && !perceptionData.getLeftVehicle().isPresent()) {
            lane--;
            position.setLane(lane);
            System.out.println(getLocalName() + " changing to left lane.");
        } else if (lane < road.getLanes() - 1 && !perceptionData.getRightVehicle().isPresent()) {
            lane++;
            position.setLane(lane);
            System.out.println(getLocalName() + " changing to right lane.");
        }
    }

    private void moveWithOffset(RoadSegment road) {
        double roadAngle = road.getAngle();
        double offset = road.getLaneOffset(lane);
        double perpAngle = roadAngle + Math.PI / 2.0;

        // Advance along the road axis
        position.setX(position.getX() + Math.cos(roadAngle) * speed);
        position.setY(position.getY() + Math.sin(roadAngle) * speed);

        // Smootly align with lane offset (simple P-controller style)
        double targetX = position.getX() + Math.cos(perpAngle) * offset;
        double targetY = position.getY() + Math.sin(perpAngle) * offset;

        // In this simple model, we just re-apply the fixed offset to the raw position
        // But to make it cleaner, the 'position' stored in Environment should include
        // the offset
        // We calculate position as (RoadStart + distanceAlongRoad) + OffsetVector
    }

    private void triggerReroute() {
        if (System.currentTimeMillis() - lastRerouteTime < 5000)
            return; // Once per 5s

        Environment env = Environment.getInstance();
        Intersection nextInter = findNextIntersectionForRoad(env, currentRoadId);
        if (nextInter != null && destinationInterId != null) {
            plannedPath = Pathfinder.findPath(env.getIntersections(), env.getRoads(),
                    nextInter.getId(), destinationInterId);
            lastRerouteTime = System.currentTimeMillis();
            System.out.println(getLocalName() + " rerouted. Path size: " + plannedPath.size());
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
