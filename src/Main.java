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

        // 3. Initialize JADE
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        AgentContainer container = rt.createMainContainer(p);

        try {
            // 4. Launch Traffic Lights at Intersection
            // Light 1 for horizontal traffic (R1)
            AgentController light1 = container.createNewAgent("Light_R1",
                    "com.traffic.agents.TrafficLightAgent", new Object[] { "480.0", "180.0", "R1" });
            light1.start();

            // Light 2 for vertical traffic (R4) - Offset by 13s (Green + Yellow of R1)
            AgentController light2 = container.createNewAgent("Light_R4",
                    "com.traffic.agents.TrafficLightAgent", new Object[] { "480.0", "150.0", "R4", "13000" });
            light2.start();

            // 5. Launch Vehicle Spawner
            AgentController spawner = container.createNewAgent("Spawner",
                    "com.traffic.agents.VehicleSpawnerAgent", null);
            spawner.start();

            // 6. Launch Metrics Collection
            AgentController metrics = container.createNewAgent("Metrics",
                    "com.traffic.agents.MetricsAgent", null);
            metrics.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}