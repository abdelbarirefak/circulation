package com.traffic.logic;

import com.traffic.model.Intersection;
import com.traffic.model.RoadSegment;
import java.util.*;

public class Pathfinder {
    public static List<String> findPath(Map<String, Intersection> intersections, Map<String, RoadSegment> roads,
            String startInterId, String endInterId) {
        if (startInterId == null || endInterId == null || startInterId.equals(endInterId)) {
            return new ArrayList<>();
        }

        // Dijkstra's Algorithm for weighted shortest path
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> parentRoad = new HashMap<>();
        Map<String, String> parentInter = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        for (String id : intersections.keySet()) {
            dist.put(id, Double.MAX_VALUE);
        }

        dist.put(startInterId, 0.0);
        pq.add(new Node(startInterId, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String u = current.id;

            if (u.equals(endInterId))
                break;
            if (current.distance > dist.get(u))
                continue;

            Intersection inter = intersections.get(u);
            if (inter == null)
                continue;

            for (RoadSegment road : inter.getOutgoingRoads()) {
                String v = findNextIntersection(intersections, road);
                if (v == null)
                    continue;

                // Weight = physical length * (1 + congestion factor) + yield friction
                int vehicleCount = com.traffic.environment.Environment.getInstance().countVehiclesOnRoad(road.getId());
                double congestionFactor = vehicleCount * 0.5; // Each car adds 50% "virtual length"
                double yieldFriction = road.isYieldTarget() ? 50.0 : 0.0; // Entering roundabout adds "cost"
                double weight = (road.getLength() + yieldFriction) * (1.0 + congestionFactor);

                double newDist = dist.get(u) + weight;
                if (newDist < dist.get(v)) {
                    dist.put(v, newDist);
                    parentInter.put(v, u);
                    parentRoad.put(v, road.getId());
                    pq.add(new Node(v, newDist));
                }
            }
        }

        // Reconstruct path of road IDs
        List<String> path = new ArrayList<>();
        String curr = endInterId;
        while (parentRoad.containsKey(curr)) {
            path.add(0, parentRoad.get(curr));
            curr = parentInter.get(curr);
        }

        return path;
    }

    private static class Node {
        String id;
        double distance;

        Node(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }

    private static String findNextIntersection(Map<String, Intersection> intersections, RoadSegment road) {
        for (Intersection inter : intersections.values()) {
            if (inter.getIncomingRoads().contains(road)) {
                return inter.getId();
            }
        }
        return null;
    }
}
