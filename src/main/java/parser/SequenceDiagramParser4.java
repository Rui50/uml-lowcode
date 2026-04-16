package parser;

import com.sdmetrics.model.*;
import com.sdmetrics.util.XMLParser;
import model.sequence.*;

import java.util.*;

public class SequenceDiagramParser4 {
    private Model model;
    private MetaModel metaModel;

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

    // parse de uma interação, incluindo lifelines e mensagens
    // construi a arvore de fragments (Combined fragments com operands e guards)
    // mapeia os eventos para operands(para saber em que operand cada mensagem esta)
    // coloca as mensagens no sitio
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

        // criar Fragment/Operand objects
        // um fragment é um combined fragment, um operand é um branch desse combined fragment
        Map<String, Fragment> combFragMap = new HashMap<>();
        Map<String, Operand> operandMap = new HashMap<>();
        buildFragmentObjects(combFragMap, operandMap);

        Map<String, Message> sendEventToMessage = parseMessages(element, interaction);

        // construir a hierarquia de elementos (mensagens e fragments) da interação
        List<ModelElement> allOperands = model.getAcceptedElements(metaModel.getType("interactionoperand"));
        for (ModelElement operandElem : allOperands) {
            Operand op = operandMap.get(operandElem.getXMIID());
            if (op == null) continue;
            populateElementsInOrder(operandElem, op.getElements(), combFragMap, sendEventToMessage);
        }

        // interaction (top-level)
        populateElementsInOrder(element, interaction.getElements(), combFragMap, sendEventToMessage);

        // depois de popular elements por ordem do XMI, resolver nested CFs que a VP
        // emite como packagedElement (fora de operand.fragments) — append ao fim do operand pai
        List<ModelElement> allCombFrags = model.getAcceptedElements(metaModel.getType("combinedfragment"));
        if (allCombFrags != null) {
            for (ModelElement combFragElem : allCombFrags) {
                ModelElement owner = combFragElem.getOwner();
                if (owner == null) continue;
                Operand parent = operandMap.get(owner.getXMIID());
                if (parent == null) continue;
                Fragment nested = combFragMap.get(combFragElem.getXMIID());
                if (nested == null) continue;
                if (!parent.getElements().contains(nested)) {
                    parent.getElements().add(nested);
                }
            }
        }

        // Context class
        if (!interaction.getLifelines().isEmpty()) {
            interaction.setContextClass(interaction.getLifelines().get(0).getType());
        }
        return interaction;
    }

    /*
     * constroi os fragmentos iniciais a partir dos CombinedFragments
     * 1 - Começa por criar um fragment para cada CombinedFragment, preenchendo o operador e guardasdos operandos
     * 2 - Resolve nesting, para cada CF, verificar se o seu owner é um operand.
     *     Se for, está nested, adiciona-o aos elements desse operand.
     *     Se não, é um fragmento raiz, adiciona-o à lista de rootCombinedFragments

     * SDMetrics nao tem order, pode devolver em qualquer ordem, assim corrigo isso

     */
    private void buildFragmentObjects(Map<String, Fragment> combFragMap, Map<String, Operand> operandMap) {
        List<ModelElement> allCombFrags = model.getAcceptedElements(metaModel.getType("combinedfragment"));
        if (allCombFrags == null) return;

        // Começar por criar fragment para cada CF e operand para cada operand
        for (ModelElement combFragElem : allCombFrags) {
            Fragment frag = new Fragment();
            // operator é o "tipo" -> alt etc
            frag.setOperator(combFragElem.getPlainAttribute("operator"));
            frag.setId(combFragElem.getXMIID());

            combFragMap.put(frag.getId(), frag);

            // operands sao os branchs
            Collection<?> operands = combFragElem.getSetAttribute("operands");
            if (operands != null) {
                for (Object operandObj : operands) {
                    ModelElement operandElem = (ModelElement) operandObj;
                    Operand operand = new Operand();
                    operand.setId(operandElem.getXMIID());
                    // guard é ref para um LiteralString — dereferenciar para obter o value
                    ModelElement guardEl = (ModelElement) operandElem.getRefAttribute("guard");
                    if (guardEl != null) {
                        operand.setGuard(guardEl.getPlainAttribute("value"));
                    }
                    frag.getOperands().add(operand);
                    operandMap.put(operand.getId(), operand); // mapear id do operand para o fragmento
                }
            }
        }
    }

    private Map<String, Message> parseMessages(ModelElement element, Interaction interaction) {
        Map<String, Message> sendEventToMessage = new HashMap<>();
        Collection<?> messages = element.getSetAttribute("messages");
        if (messages == null) return sendEventToMessage;

        List<ModelElement> sorted = new ArrayList<>();
        for (Object o : messages) sorted.add((ModelElement) o);
        sorted.sort(ModelElement.getComparator());

        int seq = 0;
        for (ModelElement msgEl : sorted) {
            Message msg = parseMessage(msgEl, interaction);
            if (msg == null) continue;
            msg.setSequenceNumber(seq++);
            ModelElement sendEvt = (ModelElement) msgEl.getRefAttribute("sendevent");
            if (sendEvt != null) sendEventToMessage.put(sendEvt.getXMIID(), msg);
        }
        return sendEventToMessage;
    }

    /**
     * itera a collection "fragments" de um container (Interaction ou InteractionOperand)
     * InteractionOperand = ramo dentro de um combfrag
     *
     * itera em ordem do XMI e adiciona mensagens e nested frags à lista de elementos do container.
     *
     * - combinedfragment -> adiciona o Fragment correspondente
     *  - occurrencespec   -> se for sendEvent de uma Message conhecida, adiciona-a
     */
    private void populateElementsInOrder(ModelElement container, List<Object> targetList,
                                         Map<String, Fragment> combFragMap,
                                         Map<String, Message> sendEventToMessage) {
        System.out.println(">> walking container id=" + container.getXMIID()
                + " type=" + container.getType().getName());
        Collection<?> fragments = container.getSetAttribute("fragments");
        if (fragments == null) {
            System.out.println("   (no fragments)");
            return;
        }
        System.out.println("   fragments.size=" + fragments.size());

        for (Object obj : fragments) {
            ModelElement entry = (ModelElement) obj;
            String typeName = entry.getType().getName();
            System.out.println("   child: type=" + typeName + " id=" + entry.getXMIID());

            if ("combinedfragment".equals(typeName)) {
                Fragment frag = combFragMap.get(entry.getXMIID());
                System.out.println("     -> CF lookup: " + (frag != null ? "HIT" : "MISS"));
                if (frag != null) targetList.add(frag);
            } else if ("occurrencespec".equals(typeName)) {
                Message msg = sendEventToMessage.get(entry.getXMIID());
                System.out.println("     -> MOS lookup: " + (msg != null ? "HIT msg=" + msg.getName() : "miss (receiveEvent or other iteraction)"));
                if (msg != null) targetList.add(msg);
            } else {
                System.out.println("     -> unknown type, skipped");
            }
        }
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

        System.out.println("Parsed lifeline: " + lifeline);

        return lifeline;
    }

    // parse messages
    private Message parseMessage(ModelElement element, Interaction interaction) {
        Message message = new Message();

        message.setId(element.getXMIID());

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

    /***
     * Percorre a coleção "fragments" de uma interação
     * na ordem do xmi e coloca mensagens e nested frags na lista
     *
     * uma entry pode ser
     *   combinedfragment: procura o Fragment ja construido e adiciona-o
     *     - occurrencespec:   é um send/receive event de uma Message. Se for o sendEvent
     *                       de uma Message conhecida, adiciona essa Message.
     *                       ReceiveEvents sao ignorados porque nao estao indexados
     *                       em messagesBySendEvent.
        */
    private void placeContents(ModelElement containerElem, List<Object> elementsOut, Map<String, Fragment> combFragMap,
                               Map<String, Message> messagesBySendEvent,
                               Set<String> placedMsgIds) {
        Collection<?> fragments = containerElem.getSetAttribute("fragments");
        if (fragments == null) return;

        for (Object obj : fragments) {
            ModelElement entry = (ModelElement) obj;
            String typeName = entry.getType().getName();

            if ("combinedfragment".equals(typeName)) {
                Fragment frag = combFragMap.get(entry.getXMIID());
                if (frag != null) elementsOut.add(frag);
            } else if ("occurrencespec".equals(typeName)) {
                Message msg = messagesBySendEvent.get(entry.getXMIID());
                if (msg != null && placedMsgIds.add(msg.getId())) {
                    elementsOut.add(msg);
                }
            }
        }
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
}
