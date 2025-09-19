package smagrs.agents;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.domain.FIPANames;
import smagrs.model.*;

import java.util.*;

public class RoomAgent extends Agent {
    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String roomName;
    private int capacity;
    private Set<String> equipment;

    /** Réservations confirmées */
    private final List<TimeSlot> bookings = new ArrayList<>();
    /** Retenues temporaires pendant un tour de CNP */
    private final Set<TimeSlot> holds = new HashSet<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        roomName = getLocalName();
        capacity = (int) args[0];
        //noinspection unchecked
        equipment = new HashSet<>((Collection<String>) args[1]);

        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP)
        );

        addBehaviour(new ContractNetResponder(this, mt) {

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                try {
                    Request req = om.readValue(cfp.getContent(), Request.class);

                    // indisponible si déjà booké ou retenu
                    if (!isFeasible(req) || isHeld(req.slot)) {
                        ACLMessage refuse = cfp.createReply();
                        refuse.setPerformative(ACLMessage.REFUSE);
                        refuse.setContent("NOT-AVAILABLE");
                        return refuse;
                    }

                    // hold pour éviter double proposition du même créneau
                    holds.add(req.slot);

                    Offer offer = score(req);
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    propose.setContent(om.writeValueAsString(offer));
                    return propose;

                } catch (Exception e) {
                    ACLMessage fail = cfp.createReply();
                    fail.setPerformative(ACLMessage.FAILURE);
                    fail.setContent("BAD-REQUEST: " + e.getMessage());
                    return fail;
                }
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                try {
                    Request req = om.readValue(cfp.getContent(), Request.class);
                    holds.remove(req.slot);
                    bookings.add(req.slot);

                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    // >>> Inclure le nom de la salle pour l’UI <<<
                    // Format: BOOKED:<roomName>:<slot>
                    inform.setContent("BOOKED:" + roomName + ":" + req.slot);
                    return inform;

                } catch (Exception e) {
                    ACLMessage fail = accept.createReply();
                    fail.setPerformative(ACLMessage.FAILURE);
                    fail.setContent("BOOKING-ERROR");
                    return fail;
                }
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                try {
                    Request req = om.readValue(cfp.getContent(), Request.class);
                    holds.remove(req.slot);
                } catch (Exception ignore) { }
            }
        });
    }

    private boolean isFeasible(Request r) {
        if (capacity < r.capacity) return false;
        if (!equipment.containsAll(r.equipment)) return false;
        for (TimeSlot t : bookings) if (t.overlaps(r.slot)) return false;
        return true;
    }

    private boolean isHeld(TimeSlot s) {
        for (TimeSlot t : holds) if (t.overlaps(s)) return true;
        return false;
    }

    private Offer score(Request r) {
        double capFit = Math.min(1.0, (double) capacity / Math.max(1, r.capacity));
        double eqFit  = (double) intersectionCount(equipment, r.equipment) / Math.max(1, r.equipment.size());
        double score  = 0.7 * capFit + 0.3 * eqFit;
        double price  = 100.0 / (1.0 + score);
        return new Offer(roomName, price, score);
    }

    private static int intersectionCount(Set<String> a, Set<String> b) {
        int n = 0; for (String s : b) if (a.contains(s)) n++; return n;
    }
}
