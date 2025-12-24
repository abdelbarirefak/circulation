
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import com.traffic.gui.SimulationWindow;

public class Main {
    public static void main(String[] args) {
        // 1. Start the GUI
        new SimulationWindow();

        // 2. Initialize JADE Runtime
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        // p.setParameter(Profile.MAIN_HOST, "localhost");
        // p.setParameter(Profile.GUI, "true"); // Optional JADE GUI

        AgentContainer mainContainer = rt.createMainContainer(p);

        try {
            // 3. Launch Traffic Light Agents
            AgentController light1 = mainContainer.createNewAgent("Light1",
                    "com.traffic.agents.TrafficLightAgent", new Object[] { "500.0", "0" });
            light1.start();

            AgentController light2 = mainContainer.createNewAgent("Light2",
                    "com.traffic.agents.TrafficLightAgent", new Object[] { "900.0", "1" });
            light2.start();

            // 4. Launch Car Agents
            for (int i = 1; i <= 5; i++) {
                double startX = i * 100.0;
                int lane = (i % 2);
                AgentController car = mainContainer.createNewAgent("Car" + i,
                        "com.traffic.agents.CarAgent", new Object[] { String.valueOf(startX), String.valueOf(lane) });
                car.start();
                Thread.sleep(500); // Staggered start
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}