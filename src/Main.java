import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import com.traffic.environment.Environment;
import com.traffic.model.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

public class Main {
        public static void main(String[] args) {
                // 1. Setup Map Network
                Environment env = Environment.getInstance();

                // Roads
                RoadSegment r1 = new RoadSegment("R1", new Position(0, 300), new Position(300, 300), 2, 50, true);
                RoadSegment r2 = new RoadSegment("R2", new Position(300, 300), new Position(300, 600), 2, 50, true);

                RoadSegment r3Curve = new RoadSegment("R3_Curve",
                                new Position(300, 300),
                                new Position(500 - 70.7, 500 - 70.7),
                                new Position(500 - 70.7, 300),
                                2, 40, true);

                RoadSegment r4toRA = new RoadSegment("R4_to_RA", new Position(500, 300), new Position(500, 400), 2, 30,
                                true);
                r4toRA.setYieldTarget(true); // Entry priority

                RoadSegment r5fromRA = new RoadSegment("R5_from_RA", new Position(600, 500), new Position(800, 500), 2,
                                30, true);
                RoadSegment r6fromRA = new RoadSegment("R6_from_RA", new Position(500, 600), new Position(500, 800), 2,
                                30, true);

                env.addRoad(r1);
                env.addRoad(r2);
                env.addRoad(r3Curve);
                env.addRoad(r4toRA);
                env.addRoad(r5fromRA);
                env.addRoad(r6fromRA);

                // Roundabout
                Roundabout ra = new Roundabout("RA1", new Position(500, 500), 100);
                env.addRoundabout(ra);

                // Intersections
                Intersection i1 = new Intersection("I1", new Position(300, 300));
                i1.addIncoming(r1);
                i1.addOutgoing(r2);
                i1.addOutgoing(r3Curve);
                env.addIntersection(i1);

                // 2. Start API Server (Modern Dashboard Bridge)
                System.out.println("Starting API Bridge initialization...");
                startApiServer(env);
                System.out.println("Main initialization complete.");

                // 3. Initialize JADE
                Runtime rt = Runtime.instance();
                Profile p = new ProfileImpl();
                AgentContainer mainContainer = rt.createMainContainer(p);
                env.setMainContainer(mainContainer);

                try {
                        mainContainer.createNewAgent("TL_R1_I1", "com.traffic.agents.TrafficLightAgent",
                                        new Object[] { "290", "300", "R1" }).start();
                        mainContainer.createNewAgent("Spawner", "com.traffic.agents.VehicleSpawnerAgent", null)
                                        .start();
                        mainContainer.createNewAgent("Metrics", "com.traffic.agents.MetricsAgent", null).start();
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private static void startApiServer(Environment env) {
                try {
                        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);
                        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

                        // Endpoint: Simulation State (SSE)
                        server.createContext("/api/stream", exchange -> {
                                exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
                                exchange.getResponseHeaders().add("Cache-Control", "no-cache");
                                exchange.getResponseHeaders().add("Connection", "keep-alive");
                                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                exchange.sendResponseHeaders(200, 0);

                                try {
                                        OutputStream os = exchange.getResponseBody();
                                        while (true) {
                                                String stateJson = serializeState(env);
                                                os.write(("data: " + stateJson + "\n\n").getBytes());
                                                os.flush();
                                                Thread.sleep(100); // 10Hz Update
                                        }
                                } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                }
                        });

                        // Endpoint: Static Map
                        server.createContext("/api/map", exchange -> {
                                System.out.println("Java: Incoming request for /api/map");
                                try {
                                        String mapJson = serializeMap(env);
                                        byte[] response = mapJson.getBytes();
                                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                                        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                        exchange.sendResponseHeaders(200, response.length);
                                        exchange.getResponseBody().write(response);
                                        exchange.getResponseBody().close();
                                        System.out.println("Java: Successfully served map data.");
                                } catch (Exception e) {
                                        System.err.println("Java Error: Failed to serialize map data.");
                                        e.printStackTrace();
                                        exchange.sendResponseHeaders(500, -1);
                                        exchange.close();
                                }
                        });

                        server.createContext("/api/control", exchange -> {
                                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                        String body = new java.io.BufferedReader(
                                                        new java.io.InputStreamReader(exchange.getRequestBody()))
                                                        .lines().collect(Collectors.joining("\n"));
                                        if (body.contains("pause"))
                                                env.setPaused(true);
                                        else if (body.contains("resume"))
                                                env.setPaused(false);
                                        if (body.contains("speed")) {
                                                if (body.contains("1.0"))
                                                        env.setTimeMultiplier(1.0);
                                                else
                                                        env.setTimeMultiplier(2.0);
                                        }
                                        exchange.sendResponseHeaders(200, -1);
                                } else {
                                        exchange.sendResponseHeaders(405, -1);
                                }
                                exchange.close();
                        });

                        server.createContext("/api/incident", exchange -> {
                                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                        String body = new java.io.BufferedReader(
                                                        new java.io.InputStreamReader(exchange.getRequestBody()))
                                                        .lines().collect(Collectors.joining("\n"));
                                        double x = 500, y = 500;
                                        if (body.contains("x")) {
                                                try {
                                                        String[] p = body.split("[,&:]");
                                                        for (String s : p) {
                                                                if (s.contains("x"))
                                                                        x = Double.parseDouble(
                                                                                        s.replaceAll("[^0-9.]", "")
                                                                                                        .trim());
                                                                if (s.contains("y"))
                                                                        y = Double.parseDouble(
                                                                                        s.replaceAll("[^0-9.]", "")
                                                                                                        .trim());
                                                        }
                                                } catch (Exception e) {
                                                }
                                        }
                                        RoadSegment target = env.getRoadAt(new Position(x, y));
                                        if (target != null && env.getMainContainer() != null) {
                                                try {
                                                        env.getMainContainer().createNewAgent(
                                                                        "Incident_" + System.currentTimeMillis(),
                                                                        "com.traffic.agents.IncidentAgent",
                                                                        new Object[] { x, y, target.getId() }).start();
                                                } catch (Exception e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                        exchange.sendResponseHeaders(200, -1);
                                } else {
                                        exchange.sendResponseHeaders(405, -1);
                                }
                                exchange.close();
                        });

                        server.start();
                        System.out.println("=================================================");
                        System.out.println("!!! DIGITAL TWIN API BRIDGE IS LIVE !!!");
                        System.out.println("Listening on: http://localhost:8085");
                        System.out.println("Map Data: http://localhost:8085/api/map");
                        System.out.println("Live Stream: http://localhost:8085/api/stream");
                        System.out.println("=================================================");
                } catch (java.net.BindException be) {
                        System.err.println("!!! CRITICAL: PORT 8085 IS BLOCKED !!!");
                        System.err.println("The API Bridge could NOT start. Visualization will be blank.");
                        System.err.println("Please kill any process using port 8085 and restart.");
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private static String serializeState(Environment env) {
                StringBuilder sb = new StringBuilder("{ \"vehicles\": [");
                sb.append(env.getVehiclePositions().entrySet().stream().map(entry -> {
                        String id = entry.getKey();
                        Position pos = entry.getValue();
                        String thought = env.getVehicleThought(id);
                        String pathJson = env.getVehiclePath(id).stream()
                                        .map(p -> String.format(java.util.Locale.US, "[%f, %f]", p.getX(), p.getY()))
                                        .collect(Collectors.joining(", ", "[", "]"));

                        return String.format(java.util.Locale.US,
                                        "{ \"id\": \"%s\", \"x\": %.2f, \"y\": %.2f, \"lane\": %d, \"road\": \"%s\", \"thought\": \"%s\", \"path\": %s }",
                                        id, pos.getX(), pos.getY(), 0, env.getVehicleRoadId(id), thought, pathJson);
                }).collect(Collectors.joining(",")));
                sb.append("] }");
                return sb.toString();
        }

        private static String serializeMap(Environment env) {
                StringBuilder sb = new StringBuilder("{ \"roads\": [");
                sb.append(env.getRoads().values().stream().map(r -> {
                        return String.format(java.util.Locale.US,
                                        "{ \"id\": \"%s\", \"startX\": %.2f, \"startY\": %.2f, \"endX\": %.2f, \"endY\": %.2f, \"lanes\": %d }",
                                        r.getId(), r.getStart().getX(), r.getStart().getY(), r.getEnd().getX(),
                                        r.getEnd().getY(), r.getLanes());
                }).collect(Collectors.joining(",")));
                sb.append("] }");
                return sb.toString();
        }
}