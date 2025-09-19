package smagrs.agents;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import smagrs.model.Request;
import smagrs.ui.SmagrsUI;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UIAgent extends Agent {
    private SmagrsUI ui;

    /** Corrélation conversationId -> Request initiale */
    private final Map<String, Request> pending = new ConcurrentHashMap<>();

    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected void setup() {
        Object[] args = getArguments();
        ui = (SmagrsUI) args[0];

        setEnabledO2ACommunication(true, 0);

        // Récupère le JSON de l’UI (non-bloquant)
        ui.onSubmit(json -> {
            try {
                putO2AObject(json, false);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                ui.setStatus("Interrompu en déposant la demande : " + ie.getMessage());
            }
        });

        // 1) Lire la file O2A et envoyer un CFP au broker avec un conversationId unique
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override public void action() {
                Object o = getO2AObject();
                if (o == null) { block(); return; }
                String json = o.toString();

                try {
                    Request req = om.readValue(json, Request.class);
                    String cid = UUID.randomUUID().toString();
                    pending.put(cid, req);

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(new AID("broker", AID.ISLOCALNAME));
                    cfp.setProtocol(jade.domain.FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    cfp.setConversationId(cid);
                    cfp.setContent(json);
                    send(cfp);

                    ui.setStatus("CFP envoyé au broker. (cid=" + cid + ")");
                } catch (Exception ex) {
                    ui.setStatus("JSON invalide : " + ex.getMessage());
                }
            }
        });

        // 2) Afficher les INFORM/FAILURE et alimenter le tableau
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            final MessageTemplate MT = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchPerformative(ACLMessage.FAILURE)
            );
            @Override public void action() {
                ACLMessage msg = receive(MT);
                if (msg == null) { block(); return; }

                String cid = msg.getConversationId();
                ui.setStatus((msg.getPerformative()==ACLMessage.INFORM ? "OK: " : "Erreur: ") + msg.getContent());

                // Retirer la demande de pending
                Request req = (cid != null) ? pending.remove(cid) : null;

                if (msg.getPerformative() == ACLMessage.INFORM && req != null) {
                    // Calcul de l’heure de fin = start + duration (heures)
                    LocalDateTime start = req.slot.start;
                    LocalDateTime end   = start.plusHours(req.slot.duration);

                    String room = (msg.getSender() != null) ? msg.getSender().getLocalName() : "?";

                    // Remplir le tableau : Salle | Début | Fin | Capacité | Demandeur
                    ui.addBookingRow(room, start.toString(), end.toString(), req.capacity, req.requester);
                }
            }
        });
    }
}
