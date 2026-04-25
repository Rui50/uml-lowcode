package parser;

import com.sdmetrics.model.*;
import com.sdmetrics.util.XMLParser;
import model.sequence.*;

import java.util.*;

public class SequenceDiagramParser2 {
    private Model model;
    private MetaModel metaModel;
    private Map<String, String> messageNumbers = new HashMap<>();

    // igual al class parser, tenho de mudar a estrutura para os dois usarem o mesmo
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

        messageNumbers = VpParser.extractMessageNumbers(xmiFile);

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

    /**
     * Parse de uma interação
     * popula lifelines, mensagens e fragmentos em ordem do XMI
     *
     * mensagens sao primeiro porque estao em .messages, equanto que os eventos em si em .fragments
     */
    private Interaction parseInteraction(ModelElement element) {
        Interaction interaction = new Interaction();
        interaction.setId(element.getXMIID());
        interaction.setName(element.getName());

        // lifelines
        Collection<?> lifelines = element.getSetAttribute("lifelines");
        if (lifelines != null) {
            for (Object o : lifelines) {
                interaction.getLifelines().add(parseLifeline((ModelElement) o));
            }
        }

        // mensagens primeiro porque estao em interaction.messages mas os eventos estao em fragments, entao preciso de mapear os eventos para as mensagens antes de colocar as mensagens
        Map<String, Message> sendEventToMessage = parseMessages(element, interaction);

        // todos os combinedFragments
        List<ModelElement> allCombFrags = model.getAcceptedElements(metaModel.getType("combinedfragment"));
        if (allCombFrags == null) allCombFrags = Collections.emptyList();

        // iteração recursiva, constroi e coloca por orden
        buildContents(element, interaction.getElements(), sendEventToMessage, allCombFrags);

        // context class
        if (!interaction.getLifelines().isEmpty()) {
            interaction.setContextClass(interaction.getLifelines().get(0).getType());
        }
        return interaction;
    }


    /**
     * Construi recursivamente a hierarquia dos elementos (mensagens/frags)
     *
     * Junta os elementos
     *  - .fragments, inclui eventos de mensagens e combined fragments top level
     *  - combined fragments nested, porque o visual paradigm emite nested CFs como <packagedElement> em vez de <fragment>
     *
     *  depois ordena e itera: para cada cf cria fragment e operands e chama
     *  recursivamente para construir o conteúdo do operand,
     *
     */
    private void buildContents(ModelElement container, List<Object> elementsList,
                               Map<String, Message> sendEventToMessage,
                               List<ModelElement> allCombFrags) {
        // junta os elementos de ambas fontes (messageoccurence e combinedfrags)
        List<ModelElement> elements = new ArrayList<>();
        Collection<?> fragments = container.getSetAttribute("fragments");
        if (fragments != null) {
            for (Object o : fragments) {
                elements.add((ModelElement) o);
            }
        }

        // combined fragments cujo owner é o container atual
        String containerId = container.getXMIID();
        for (ModelElement cf : allCombFrags) {
            ModelElement owner = cf.getOwner();
            if (owner == null || !containerId.equals(owner.getXMIID())) continue;
            if (!elements.contains(cf)) elements.add(cf);
        }

        // ordernar por ordem do xmi
        //elements.sort(ModelElement.getComparator());

        elements.sort((a, b) -> {
            String na = numberOf(a, sendEventToMessage);
            String nb = numberOf(b, sendEventToMessage);
            if (na != null && nb != null) return compareWithTheDot(na, nb);
            return ModelElement.getComparator().compare(a, b);  // fallback (CFs)
        });

        // interar e construir recursivamente
        for (ModelElement elem : elements) {
            String typeName = elem.getType().getName();
            if ("combinedfragment".equals(typeName)) {
                Fragment frag = new Fragment();
                frag.setId(elem.getXMIID());
                // operator (loop, alt, opt)
                frag.setOperator(elem.getPlainAttribute("operator"));

                // operands (guard + branches se tiver)
                Collection<?> operands = elem.getSetAttribute("operands");
                if (operands != null) {
                    for (Object o : operands) {
                        ModelElement operandElem = (ModelElement) o;
                        Operand op = new Operand();
                        op.setId(operandElem.getXMIID());
                        ModelElement guardEl = (ModelElement) operandElem.getRefAttribute("guard");
                        if (guardEl != null) op.setGuard(guardEl.getPlainAttribute("value"));

                        // recursão para construir o conteúdo do operand
                        //o operand é ele próprio um container
                        buildContents(operandElem, op.getElements(), sendEventToMessage, allCombFrags);

                        frag.getOperands().add(op);
                    }
                }
                elementsList.add(frag);
            } else if ("occurrencespec".equals(typeName)) {
                Message msg = sendEventToMessage.get(elem.getXMIID());
                if (msg != null) elementsList.add(msg);
            }
        }
    }

    private Map<String, Message> parseMessages(ModelElement element, Interaction interaction) {
        Map<String, Message> sendEventToMessage = new HashMap<>();
        Collection<?> messages = element.getSetAttribute("messages");
        if (messages == null) return sendEventToMessage;

        List<ModelElement> msgs = new ArrayList<>();
        for (Object o : messages) msgs.add((ModelElement) o);

        int seq = 0;
        for (ModelElement msgEl : msgs) {
            Message msg = parseMessage(msgEl, interaction);
            if (msg == null) continue;
            msg.setSequenceNumber(seq++);
            ModelElement sendEvt = (ModelElement) msgEl.getRefAttribute("sendevent");
            if (sendEvt != null) sendEventToMessage.put(sendEvt.getXMIID(), msg);
        }
        return sendEventToMessage;
    }

    private Lifeline parseLifeline(ModelElement element) {
        Lifeline lifeline = new Lifeline();

        lifeline.setId(element.getXMIID());
        lifeline.setName(element.getName());


        ModelElement represents = (ModelElement) element.getRefAttribute("represents");
        if (represents != null) {
            // represents é um ownedAttribute — o nome é o nome da variável ("u")
            String varName = represents.getName();
            lifeline.setRepresents(varName);

            // o tipo da lifeline está no VP extension do ownedAttribute (<type value="User"/>)
            String vpType = represents.getPlainAttribute("vptype");
            if (vpType != null && !vpType.isEmpty()) {
                lifeline.setType(vpType);
            }

            System.out.println("Parsed lifeline with represents: " + lifeline);
        }
        // fallback mas n deve sequer funcionar
        else {
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

        //System.out.println("Parsed lifeline: " + lifeline);

        return lifeline;
    }

    // parse messages
    private Message parseMessage(ModelElement element, Interaction interaction) {
        Message message = new Message();

        message.setId(element.getXMIID());
        message.setVpNumber(messageNumbers.get(element.getXMIID()));

        String rawName = element.getName();
        message.setRawName(rawName);

        // parse nome, argumentos e return type do nome raw
        parseMessageSignature(message, rawName);

        // messageSort (synchronous, asynchronous, createMessage, reply)
        String messageSort = element.getPlainAttribute("sort");
        message.setMessageSort(messageSort);

        // send / receive events
        ModelElement sendEvent = (ModelElement) element.getRefAttribute("sendevent");
        ModelElement receiveEvent = (ModelElement) element.getRefAttribute("receiveevent");

        // encontra e associa as lifelines associadas aos eventos
        if (sendEvent != null) {
            ModelElement senderLifeline = (ModelElement) sendEvent.getRefAttribute("covered");
            if (senderLifeline != null) {
                String senderLifelineId = senderLifeline.getXMIID();
                message.setSource(findLifeline(interaction, senderLifelineId));
            }
        }

        if (receiveEvent != null) {
            ModelElement receiverLifeline = (ModelElement) receiveEvent.getRefAttribute("covered");
            if (receiverLifeline != null) {
                String receiverLifelineId = receiverLifeline.getXMIID();
                message.setTarget(findLifeline(interaction, receiverLifelineId));
            }
        }

        System.out.println("Parsed message: " + message);

        return message;
    }

    /**
     * parsing das mensages pelo "raw name" que pode conter nome, argumentos e return type
     *   getTurma()
     *   setTurma(novaTurma: Turma)
     *   get(users: ArrayList<User>) : User
     */
    private void parseMessageSignature(Message message, String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            message.setName(rawName);
            return;
        }

        // verificar se tem assignment (var = method)
        String signaturePart = rawName;
        int equalsIndex = rawName.indexOf('=');
        int parenOpen = rawName.indexOf('(');

        // o '=' tem de vir antes do '(' para ser assignment (senão pode ser parte de um argumento)
        if (equalsIndex != -1 && (parenOpen == -1 || equalsIndex < parenOpen)) {
            message.setAssignVariable(rawName.substring(0, equalsIndex).trim());
            signaturePart = rawName.substring(equalsIndex + 1).trim();
            parenOpen = signaturePart.indexOf('(');
        }

        // sem parenteses é nome simples sem argumentos
        if (parenOpen == -1) {
            message.setName(signaturePart.trim());
            return;
        }

        // nome do metodo é tudo antes do '('
        message.setName(signaturePart.substring(0, parenOpen).trim());

        int parenClose = signaturePart.lastIndexOf(')');
        if (parenClose == -1 || parenClose <= parenOpen) {
            return;
        }

        // argumentos entre '(' e ')'
        String argsString = signaturePart.substring(parenOpen + 1, parenClose).trim();
        if (!argsString.isEmpty()) {
            // split por ',' mas respeitando generics como ArrayList<User>
            List<String> argParts = splitArguments(argsString);

            for (String argPart : argParts) {
                MessageArgument arg = parseArgument(argPart.trim());
                if (arg != null) {
                    message.getArguments().add(arg);
                }
            }
        }

        // return type: tudo depois de ')' com ':' -> " : User"
        String afterParen = signaturePart.substring(parenClose + 1).trim();
        if (afterParen.startsWith(":")) {
            String returnType = afterParen.substring(1).trim();
            if (!returnType.isEmpty()) {
                message.setReturnType(returnType);
            }
        }

        System.out.println("Parsed message signature - Name: " + message.getName()
                + ", Arguments: " + message.getArguments()
                + ", Return Type: " + message.getReturnType()
                + ", AssignVariable: " + message.getAssignVariable());
    }

    private void parseMessageSignature2(Message message, String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            message.setName(rawName);
            return;
        }

        // regex: (variavel =)? metodo(args) (: ReturnType)?
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "^(?:(.+?)\\s*=\\s*)?(.+?)\\((.*)\\)(?:\\s*:\\s*(.+))?$"
        );
        java.util.regex.Matcher m = pattern.matcher(rawName.trim());

        if (m.matches()) {
            if (m.group(1) != null) {
                message.setAssignVariable(m.group(1).trim());
            }
            message.setName(m.group(2).trim());

            // parse argumentos
            String argsString = m.group(3).trim();
            if (!argsString.isEmpty()) {
                List<String> argParts = splitArguments(argsString);
                for (String argPart : argParts) {
                    MessageArgument arg = parseArgument(argPart.trim());
                    if (arg != null) {
                        message.getArguments().add(arg);
                    }
                }
            }

            if (m.group(4) != null) {
                message.setReturnType(m.group(4).trim());
            }
        } else {
            // sem parenteses — nome simples
            message.setName(rawName.trim());
        }

        System.out.println("Parsed message signature2 - Name: " + message.getName()
                + ", Arguments: " + message.getArguments()
                + ", Return Type: " + message.getReturnType()
                + ", AssignVariable: " + message.getAssignVariable());
    }

    /**
     * parsing de um argumento, que pode ser só "name" ou "name: Type"
     */
    private MessageArgument parseArgument(String argString) {
        if (argString == null || argString.isEmpty()) {
            return null;
        }

        MessageArgument arg = new MessageArgument();

        // procurar ':' que não esteja dentro de <>
        int colonIndex = findColonOutsideGenerics(argString);

        if (colonIndex != -1) {
            // name: Type
            arg.setName(argString.substring(0, colonIndex).trim());
            arg.setType(argString.substring(colonIndex + 1).trim());
        } else {
            // só nome
            arg.setName(argString.trim());
        }

        return arg;
    }
    /**
     * Divide uma string por , mas ignora as vírgulas dentro de < > (para listas e maps)
     */
    private List<String> splitArguments(String input) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(input.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(input.substring(start));

        return parts;
    }

    /**
     * Encontra o primeiro ':' que não esteja dentro de < > (para evitar confundir com generics como List<User>)
     */
    private int findColonOutsideGenerics(String input) {
        int depth = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ':' && depth == 0) return i;
        }
        return -1;
    }

    private Lifeline findLifeline(Interaction interaction, String lifelineId) {
        for (Lifeline lifeline : interaction.getLifelines()) {
            if (lifeline.getId().equals(lifelineId)) {
                return lifeline;
            }
        }
        return null;
    }

    private String numberOf(ModelElement el, Map<String, Message> sendEventToMessage) {
        Message m = sendEventToMessage.get(el.getXMIID());
        return m != null ? m.getVpNumber() : null;
    }

    private int compareWithTheDot(String a, String b) {
        String[] ap = a.split("\\.");
        String[] bp = b.split("\\.");
        int n = Math.min(ap.length, bp.length);
        for (int i = 0; i < n; i++) {
            int d = Integer.parseInt(ap[i]) - Integer.parseInt(bp[i]);
            if (d != 0) return d;
        }
        return ap.length - bp.length;
    }
}
