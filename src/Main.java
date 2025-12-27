import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import com.traffic.gui.SimulationWindow;
import com.traffic.environment.Environment;
import com.traffic.model.*;

public class Main {
        public static void main(String[] args) {
                // 1. Setup Map Network
                Environment env = Environment.getInstance();

                // Road 1: Left to Right (Incoming to I1)
                RoadSegment road1 = new RoadSegment("R1", new Position(50, 200), new Position(500, 200), 2, 5.0, true);
                // Road 4: Top to Bottom (Incoming to I1)
                RoadSegment road4 = new RoadSegment("R4", new Position(500, -50), new Position(500, 200), 2, 5.0, true);

                // Road 2: Vertical down from intersection (Outgoing)
                RoadSegment road2 = new RoadSegment("R2", new Position(500, 200), new Position(500, 500), 2, 4.0, true);
                // Road 3: Right to Left from intersection (Outgoing)
                RoadSegment road3 = new RoadSegment("R3", new Position(500, 200), new Position(900, 200), 2, 6.0, true);

                Intersection inter = new Intersection("I1", new Position(500, 200));
                inter.addIncoming(road1);
                inter.addIncoming(road4);
                inter.addOutgoing(road2);
                inter.addOutgoing(road3);

                env.addRoad(road1);
                env.addRoad(road2);
                env.addRoad(road3);
                env.addRoad(road4);
                env.addIntersection(inter);

                // 2. Start GUI
                new SimulationWindow();

                // Create Roads
                env.addRoad(new RoadSegment("R1", new Position(0, 300), new Position(300, 300), 2, 50, true));
                env.addRoad(new RoadSegment("R2", new Position(300, 300), new Position(300, 600), 2, 50, true));

                // Curved Road Segment (Bezier: R3 connects R1-end to RA1 boundary)
                env.addRoad(new RoadSegment("R3_Curve",
                                new Position(300, 300),
                                new Position(500 - 70.7, 500 - 70.7), // End on circle boundary (45 deg)
                                new Position(500 - 70.7, 300), // Control Point
                                2, 40, true));

                // Central Roundabout
                Roundabout ra = new Roundabout("RA1", new Position(500, 500), 100);
                env.addRoundabout(ra);

                // Roads connecting to Roundabout
                env.addRoad(new RoadSegment("R4_to_RA", new Position(500, 500 - 200), new Position(500, 500 - 100), 2,
                                30, true));
                env.addRoad(new RoadSegment("R5_from_RA", new Position(500 + 100, 500), new Position(500 + 300, 500), 2,
                                30, true));

                // Add a south-connecting road for symmetry
                env.addRoad(new RoadSegment("R6_from_RA", new Position(500, 500 + 100), new Position(500, 500 + 300), 2,
                                30, true));

                // Intersections
                Intersection i1 = new Intersection("I1", new Position(300, 300));
                i1.addIncoming(env.getRoads().get("R1"));
                i1.addOutgoing(env.getRoads().get("R2"));
                i1.addOutgoing(env.getRoads().get("R3_Curve"));
                env.addIntersection(i1);

                // 3. Initialize JADE
                Runtime rt = Runtime.instance();
                Profile p = new ProfileImpl();
                AgentContainer mainContainer = rt.createMainContainer(p);

                // Agents
                try {
                        // Traffic Lights
                        AgentController t1 = mainContainer.createNewAgent("TL_R1_I1",
                                        "com.traffic.agents.TrafficLightAgent",
                                        new Object[] { "290", "300", "R1" });
                        t1.start();

                        // Spawner (Updated for 5-vehicle limit)
                        AgentController spawner = mainContainer.createNewAgent("Spawner",
                                        "com.traffic.agents.VehicleSpawnerAgent",
                                        null);
                        spawner.start();

                        // Metrics agent
                        AgentController metrics = mainContainer.createNewAgent("Metrics",
                                        "com.traffic.agents.MetricsAgent", null);
                        metrics.start();

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}