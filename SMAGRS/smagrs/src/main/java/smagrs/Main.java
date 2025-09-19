package smagrs;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import smagrs.agents.*;
import smagrs.ui.SmagrsUI;

public class Main {
    public static void main(String[] args) throws Exception {
        // Lancer l'UI Swing
        SmagrsUI ui = new SmagrsUI();
        ui.setVisible(true);

        // JADE Runtime
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true"); // GUI JADE optionnel

        ContainerController cc = rt.createMainContainer(p);

        // Démarrer les agents
        AgentController broker = cc.createNewAgent("broker", BrokerAgent.class.getName(), null);
        broker.start();

        AgentController roomA = cc.createNewAgent("room-A", RoomAgent.class.getName(), new Object[]{40, java.util.List.of("projecteur", "tableau")});
        AgentController roomB = cc.createNewAgent("room-B", RoomAgent.class.getName(), new Object[]{20, java.util.List.of("tableau")});
        AgentController roomC = cc.createNewAgent("room-C", RoomAgent.class.getName(), new Object[]{15, java.util.List.of("projecteur")});
        roomA.start();
        roomB.start();
        roomC.start();

        AgentController uiAgent = cc.createNewAgent("ui", UIAgent.class.getName(), new Object[]{ui});
        uiAgent.start();
    }
}
