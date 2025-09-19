package smagrs.agents;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;
import smagrs.model.Offer;
import smagrs.model.Request;

import java.util.List;
import java.util.Vector;

public class BrokerAgent extends Agent {

    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final List<AID> rooms = List.of(
            new AID("room-A", AID.ISLOCALNAME),
            new AID("room-B", AID.ISLOCALNAME),
            new AID("room-C", AID.ISLOCALNAME)
    );

    @Override
    protected void setup() {
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            final MessageTemplate MT = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.CFP)
            );

            @Override
            public void action() {
                ACLMessage cfpFromUI = receive(MT);
                if (cfpFromUI == null) { block(); return; }

                try {
                    // parse pour valider
                    om.readValue(cfpFromUI.getContent(), Request.class);

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    cfp.setContent(cfpFromUI.getContent());
                    cfp.setConversationId(cfpFromUI.getConversationId()); // garder la corrélation
                    rooms.forEach(cfp::addReceiver);

                    addBehaviour(new ContractNetInitiator(myAgent, cfp) {
                        @Override
                        protected void handleAllResponses(Vector responses, Vector acceptances) {
                            ACLMessage bestResp = null;
                            Offer bestOffer = null;

                            for (Object o : responses) {
                                ACLMessage resp = (ACLMessage) o;
                                if (resp.getPerformative() == ACLMessage.PROPOSE) {
                                    try {
                                        Offer offer = om.readValue(resp.getContent(), Offer.class);
                                        if (bestOffer == null || offer.getScore() > bestOffer.getScore()) {
                                            bestOffer = offer;
                                            bestResp  = resp;
                                        }
                                    } catch (Exception ignore) { }
                                }
                            }

                            for (Object o : responses) {
                                ACLMessage resp = (ACLMessage) o;
                                ACLMessage reply = resp.createReply();
                                reply.setPerformative(resp == bestResp
                                        ? ACLMessage.ACCEPT_PROPOSAL
                                        : ACLMessage.REJECT_PROPOSAL);
                                acceptances.add(reply);
                            }

                            if (bestResp == null) {
                                sendFailureToUI(cfpFromUI, "Aucune salle disponible pour cette demande.");
                            }
                        }

                        @Override
                        protected void handleInform(ACLMessage inform) {
                            // >>> Relayer tel quel (contient BOOKED:<room>:<slot>)
                            ACLMessage toUI = cfpFromUI.createReply();
                            toUI.setConversationId(cfpFromUI.getConversationId());
                            toUI.setPerformative(ACLMessage.INFORM);
                            toUI.setContent(inform.getContent());
                            send(toUI);
                        }

                        @Override
                        protected void handleFailure(ACLMessage failure) {
                            ACLMessage toUI = cfpFromUI.createReply();
                            toUI.setConversationId(cfpFromUI.getConversationId());
                            toUI.setPerformative(ACLMessage.FAILURE);
                            toUI.setContent("Échec : " + failure.getContent());
                            send(toUI);
                        }
                    });

                } catch (Exception e) {
                    sendFailureToUI(cfpFromUI, "CFP invalide : " + e.getMessage());
                }
            }
        });
    }

    private void sendFailureToUI(ACLMessage original, String text) {
        ACLMessage toUI = original.createReply();
        toUI.setConversationId(original.getConversationId());
        toUI.setPerformative(ACLMessage.FAILURE);
        toUI.setContent(text);
        send(toUI);
    }
}
