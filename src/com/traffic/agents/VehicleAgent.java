package com.traffic.agents;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import com.traffic.environment.Environment;
import com.traffic.model.*;
import com.traffic.logic.Pathfinder;

public class VehicleAgent extends BaseTrafficAgent {
    protected String currentRoadId;
    protected int lane;
    protected double progress; // Distance along current road
    protected DrivingProfile profile;
    protected String destinationInterId;
    protected List<String> plannedPath = new ArrayList<>();

    protected double currentMaxSpeed;
    protected double currentSafetyRadius;
    protected PerceptionData perceptionData = new PerceptionData();
    protected String currentAction = "Cruising";
    protected long lastRerouteTime = 0;

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

        Environment env = Environment.getInstance();
        if (destinationInterId != null) {
            Intersection startInter = findNextIntersectionForRoad(env, currentRoadId);
            if (startInter != null) {
                plannedPath = Pathfinder.findPath(env.getIntersections(), env.getRoads(),
                        startInter.getId(), destinationInterId);
            }
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
        perceptionData = new PerceptionData();

        // 1. Vehicles
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

        // 2. Traffic Lights (Simple proximity)
        String lightName = env.getNearestLight(position, 80.0);
        if (lightName != null) {
            perceptionData.setTrafficLight(new PerceptionData.LightInfo(lightName, env.getLightStates().get(lightName),
                    position.distanceTo(env.getLightPositions().get(lightName))));
        }

        // 3. Incidents
        for (Environment.Incident inc : env.getActiveIncidents()) {
            if (inc.roadId.equals(currentRoadId) && position.distanceTo(inc.position) < perceptionRadius) {
                perceptionData.setIncidentAhead(true);
            }
        }

        // 4. Roundabout
        if (road.isYieldTarget())
            checkRoundaboutYield(env, road);
    }

    private void checkRoundaboutYield(Environment env, RoadSegment entryRoad) {
        Intersection junction = findNextIntersectionForRoad(env, entryRoad.getId());
        if (junction == null)
            return;
        for (String otherName : env.getNearbyAgents(position, 100.0, getLocalName())) {
            String otherRoadId = env.getVehicleRoadId(otherName);
            if (otherRoadId != null && !otherRoadId.equals(entryRoad.getId())) {
                perceptionData.setEmergencyBrake(true);
                currentAction = "Yielding to Circle";
                return;
            }
        }
    }

    @Override
    protected void handleMessage(ACLMessage msg) {
        if (msg.getContent().equals("HAZARD_AHEAD"))
            triggerReroute();
    }

    @Override
    protected void decide() {
        Environment env = Environment.getInstance();
        RoadSegment road = env.getRoads().get(currentRoadId);
        if (road == null)
            return;

        double prevSpeed = speed;
        boolean shouldBrake = false;
        double accel = profile.getAccelRate();

        if (perceptionData.isEmergencyBrake()) {
            shouldBrake = true;
            accel = -profile.getBrakeRate() * 2.0;
        } else if (perceptionData.getLeadVehicle().isPresent()) {
            double dist = perceptionData.getLeadVehicle().get().distance;
            if (dist < (speed * 1.5 + 20.0)) {
                shouldBrake = true;
                accel = -profile.getBrakeRate();
                currentAction = "Following Lead";
            }
        } else if (perceptionData.getTrafficLight().isPresent()) {
            PerceptionData.LightInfo light = perceptionData.getTrafficLight().get();
            if (light.state != LightState.GREEN && light.distance > 10.0) {
                shouldBrake = true;
                accel = -profile.getBrakeRate();
                currentAction = "Stopping at Light";
            }
        }

        if (!shouldBrake) {
            speed = Math.min(currentMaxSpeed, speed + accel);
            currentAction = "Cruising";
        } else {
            speed = Math.max(0, speed + accel);
        }

        progress += speed;
        if (progress >= road.getLength()) {
            handleIntersectionEntry(env, road);
            progress = 0;
        } else {
            updatePhysicalPosition(road);
        }

        // Sync with transparency layer
        List<Position> pathPositions = new ArrayList<>();
        if (plannedPath != null) {
            for (String rid : plannedPath) {
                RoadSegment rs = env.getRoads().get(rid);
                if (rs != null)
                    pathPositions.add(rs.getEnd());
            }
        }
        env.updateVehicleState(getLocalName(), position, currentRoadId, speed - prevSpeed, currentAction,
                pathPositions);
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

    private void handleIntersectionEntry(Environment env, RoadSegment currentRoad) {
        if (!plannedPath.isEmpty()) {
            String nextId = plannedPath.remove(0);
            RoadSegment nextRoad = env.getRoads().get(nextId);
            if (nextRoad != null) {
                currentRoadId = nextId;
                position.setX(nextRoad.getStart().getX());
                position.setY(nextRoad.getStart().getY());
                return;
            }
        }

        Intersection inter = findNextIntersectionForRoad(env, currentRoadId);
        if (inter != null && !inter.getOutgoingRoads().isEmpty()) {
            RoadSegment nextRoad = inter.getOutgoingRoads().get(0);
            currentRoadId = nextRoad.getId();
            position.setX(nextRoad.getStart().getX());
            position.setY(nextRoad.getStart().getY());
        } else {
            env.removeVehicle(getLocalName());
            doDelete();
        }
    }

    private Intersection findNextIntersectionForRoad(Environment env, String roadId) {
        RoadSegment road = env.getRoads().get(roadId);
        if (road == null)
            return null;
        for (Intersection inter : env.getIntersections().values()) {
            if (inter.getIncomingRoads().contains(road))
                return inter;
        }
        return null;
    }

    private void triggerReroute() {
        if (System.currentTimeMillis() - lastRerouteTime < 5000)
            return;
        Environment env = Environment.getInstance();
        Intersection nextInter = findNextIntersectionForRoad(env, currentRoadId);
        if (nextInter != null && destinationInterId != null) {
            plannedPath = Pathfinder.findPath(env.getIntersections(), env.getRoads(), nextInter.getId(),
                    destinationInterId);
            lastRerouteTime = System.currentTimeMillis();
            currentAction = "Rerouting around Hazard";
        }
    }

    @Override
    protected void takeDown() {
        Environment.getInstance().removeVehicle(getLocalName());
    }
}
