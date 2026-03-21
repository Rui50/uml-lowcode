package parser;

import java.util.*;

import com.sdmetrics.model.*;

import com.sdmetrics.util.XMLParser;
import model.sequence.*;

public class SequenceDiagramParser {
    private Model model;
    private MetaModel metaModel;

    public void loadModel(String xmiFile) throws Exception{

        // caminhos dos ficheiros de configuração do SDMetrics (para uml 2.0)
        String metaModelURL  = "lib/metamodel2.xml";
        String xmiTransURL = "lib/xmiTrans2_0.xml";

        // parser do SDMetrics
        XMLParser parser = new XMLParser();

        // carregar o metamodelo UML
        System.out.println("Loading meta-model from: " + metaModelURL );
        metaModel = new MetaModel();
        parser.parse(metaModelURL , metaModel.getSAXParserHandler());

        // carregar as trasnformações
        System.out.println("Loading transformations from: " + xmiTransURL);
        XMITransformations trans=new XMITransformations(metaModel);
        parser.parse(xmiTransURL, trans.getSAXParserHandler());

        // ler o ficheiro XMI com o modelo UML
        System.out.println("Loading model from: " + xmiFile);
        model = new Model(metaModel);
        XMIReader xmiReader = new XMIReader(trans, model);
        parser.parse(xmiFile, xmiReader);

        System.out.println("Model loaded successfully!");

    }

    public List<Interaction> parseInteractions() {
        List<Interaction> interactions = new ArrayList<>();

        List<ModelElement> interactionElements = model.getAcceptedElements(
                metaModel.getType("interaction")
        );

        System.out.println("Found " + interactionElements.size() + " interactions in the model.");

        for (ModelElement element : interactionElements) {
            Interaction interaction = parseInteraction(element);
            if (interaction != null) {
                interactions.add(interaction);
            }
        }

        return interactions;
    }

    private Interaction parseInteraction(ModelElement element) {
        Interaction interaction = new Interaction();

        // nome da interação
        interaction.setId(element.getXMIID());
        interaction.setName(element.getName());

        System.out.println("Parsing interaction: " + interaction.getName() + " (ID: " + interaction.getId() + ")");

        // parse lifelines
        Collection<?> lifelines = element.getSetAttribute("lifelines");
        if (lifelines != null) {
            for (Object lifelineObj : lifelines) {
                ModelElement lifelineElement = (ModelElement) lifelineObj;
                Lifeline lifeline = parseLifeline(lifelineElement);
                interaction.getLifelines().add(lifeline);
            }
        }

        Collection<?> messages = element.getSetAttribute("messages");
        if (messages != null) {
            int sequenceNumber = 0;
            for (Object messageObj : messages) {
                ModelElement messageElement = (ModelElement) messageObj;
                Message message = parseMessage(messageElement, interaction);
                if (message != null) {
                    message.setSequenceNumber(sequenceNumber++);
                    interaction.getMessages().add(message);
                }
            }
        }

        // Determinar context class (primeira lifeline)
        if (!interaction.getLifelines().isEmpty()) {
            Lifeline firstLifeline = interaction.getLifelines().get(0);
            interaction.setContextClass(firstLifeline.getType());
        }

        return interaction;
    }

    private Lifeline parseLifeline(ModelElement element) {
        Lifeline lifeline = new Lifeline();

        lifeline.setId(element.getXMIID());
        lifeline.setName(element.getName());


        ModelElement represents = (ModelElement) element.getRefAttribute("represents");
        if (represents != null) {
            lifeline.setType(represents.getName());
            lifeline.setRepresents(represents.getName().toLowerCase());
        } else {
            // tipo
            String name = lifeline.getName();
            if (name != null) {
                if (name.startsWith(":")) {
                    // :Turma
                    lifeline.setType(name.substring(1).trim());
                    lifeline.setRepresents(name.substring(1).toLowerCase().trim());
                } else if (name.contains(":")) {
                    // novaTurma:Turma
                    String[] parts = name.split(":");
                    lifeline.setRepresents(parts[0].trim());
                    lifeline.setType(parts[1].trim());
                } else {
                    // Turma
                    lifeline.setType(name.trim());
                    lifeline.setRepresents(name.toLowerCase());
                }
            }
        }

        System.out.println("Parsed lifeline: " + lifeline);

        return lifeline;
    }

    // parse messages
    private Message parseMessage(ModelElement element, Interaction interaction) {
        Message message = new Message();

        message.setId(element.getXMIID());
        message.setName(element.getName());

        // messageSort (synchronous, asynchronous, createMessage, reply)
        String messageSort = element.getPlainAttribute("sort");
        message.setMessageSort(messageSort);

        // send / receive events
        ModelElement sendEvent = (ModelElement) element.getRefAttribute("sendevent");
        ModelElement receiveEvent = (ModelElement) element.getRefAttribute("receiveevent");

        // encontra e associa as lifelines associadas aos eventos
        if (sendEvent != null) {
            // send event tem "covered" que aponta para a lifeline
            ModelElement senderLifeline = (ModelElement) sendEvent.getRefAttribute("covered");
            if (senderLifeline != null) {
                String senderLifelineId = senderLifeline.getXMIID();
                message.setSource(findLifeline(interaction, senderLifelineId));

                System.out.println("Message " + message.getName() + " source lifeline: " + message.getSource());
            }
        }

        if (receiveEvent != null) {
            ModelElement receiverLifeline = (ModelElement) receiveEvent.getRefAttribute("covered");
            if (receiverLifeline != null) {
                String receiverLifelineId = receiverLifeline.getXMIID();
                message.setTarget(findLifeline(interaction, receiverLifelineId));
            }
        }


        // parsing dos argumentos


        return message;
    }

    private Lifeline findLifeline(Interaction interaction, String lifelineId) {
        for (Lifeline lifeline : interaction.getLifelines()) {
            if (lifeline.getId().equals(lifelineId)) {
                return lifeline;
            }
        }
        return null;
    }
}
