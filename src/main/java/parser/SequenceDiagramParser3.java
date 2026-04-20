package parser;

import com.sdmetrics.model.*;
import com.sdmetrics.util.XMLParser;
import model.sequence.*;

import java.util.*;

public class SequenceDiagramParser3 {
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

        // arvore de fragmentos
        Map<String, Operand> operandMap = new HashMap<>();
        // os combined fragments que estao no nivel mais alto
        List<Fragment> rootCombinedFragments = makeFragmentTree(operandMap);

        // map events a operands
        Map<String, String> eventToOperandId = buildEventMap(operandMap);

        // por mensagens na árvore
        placeMessages(element, interaction, operandMap, rootCombinedFragments, eventToOperandId);

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

    private List<Fragment> makeFragmentTree(Map<String, Operand> operandMap) {
        List<Fragment> rootCombinedFragments = new ArrayList<>();
        List<ModelElement> allCombFrags = model.getAcceptedElements(metaModel.getType("combinedfragment"));
        if (allCombFrags == null || allCombFrags.isEmpty()) return rootCombinedFragments;

        // Começar por criar fragment para cada CF e operand para cada operand
        Map<String, Fragment> combFragMap = new HashMap<>(); // usar para resolver nesting
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

        // resolver nesting
        // dono de um CF é um operand (existe no operandMap) -> nested dentro desse operand
        // dono não é um operand -> top-level
        for (ModelElement combFragElem : allCombFrags) {
            Fragment frag = combFragMap.get(combFragElem.getXMIID());
            ModelElement owner = combFragElem.getOwner();

            if (owner != null && operandMap.containsKey(owner.getXMIID())) {
                operandMap.get(owner.getXMIID()).addElement(frag);
            } else {
                rootCombinedFragments.add(frag);
            }
        }

        return rootCombinedFragments;
    }

    /**
     * Mapeia eventos (send/receive) a operadores
     *
     * pelo xmi cada operand contem "fragments" do tipo MessageOccurrenceSpecification
     * Este mapa vai ser para dado um sendEvent de uma mensagem, encontrar o operand onde esse evento ocorre
     * Se o sendEvent não está no mapa, a mensagem está na raiz da interaction.
     */
    private Map<String, String> buildEventMap(Map<String, Operand> operandMap){
        Map<String, String> eventToOperandId = new HashMap<>();

        List<ModelElement> allOperands = model.getAcceptedElements(metaModel.getType("interactionoperand"));
        for (ModelElement operandElem : allOperands){
            // filtrar
            if (!operandMap.containsKey(operandElem.getXMIID())) continue;

            // "fragmentos" aqui sao eventos
            Collection<?> events = operandElem.getSetAttribute("fragments");

            if(events != null){
                for (Object eventObj : events){
                    ModelElement eventElem = (ModelElement) eventObj;
                    eventToOperandId.put(eventElem.getXMIID(), operandElem.getXMIID());
                }
            }
        }
        return eventToOperandId;
    }

    /**
     * Itera as mensagens por odem e coloca cada uma no "seu lugar"
     *
     * cada mensagem
     * 1 - procura sendevent no eventMap
     * 2 - se encontrar, coloca a mensagem nesse operand e insere o "toplevel" frag na interaçºão
     * 3 - se nao encontra a mensagem é top-level, coloca-a diretamente na interação
     */

    private void placeMessages(ModelElement element,
                               Interaction interaction,
                               Map<String, Operand> operandMap,
                               List<Fragment> rootCombinedFragments,
                               Map<String, String> eventToOperandId) {

        Collection<?> messages = element.getSetAttribute("messages");
        if (messages == null) return;

        // ordenar mensagens
        List<ModelElement> sorted = new ArrayList<>();
        for (Object obj : messages) sorted.add((ModelElement) obj);
        sorted.sort(ModelElement.getComparator());

        //
        Set<String> placedFragments = new HashSet<>();
        int seq = 0;

        for (ModelElement msgElement : sorted) {
            Message msg = parseMessage(msgElement, interaction);
            if (msg == null) continue;
            msg.setSequenceNumber(seq++);

            // descobrir o operand em que a msg está
            ModelElement sendEvent = (ModelElement) msgElement.getRefAttribute("sendevent");
            String operandId = (sendEvent != null) ? eventToOperandId.get(sendEvent.getXMIID()) : null;

            if (operandId != null && operandMap.containsKey(operandId)){
                // mensagem dentro de um fragment
                operandMap.get(operandId).addElement(msg);

                // inserir o top level fragment na interação em si se ainda nao foi
                for (Fragment frag : rootCombinedFragments){
                    if (!placedFragments.contains(frag.getId()) && containsOperand(frag, operandId)) {
                        interaction.addElement(frag);
                        placedFragments.add(frag.getId());
                    }
                }
            } else {
                // mensagem na raiz
                interaction.addElement(msg);
            }
        }
    }

    /**
     * Verifica recursivamente se um Fragment contém um operand com o ID dado.
     * Percorre os operands do Fragment e os elements de cada operand (que podem conter sub-Fragments).
     */
    private boolean containsOperand(Fragment fragment, String operandId) {
        for (Operand op : fragment.getOperands()) {
            if (op.getId().equals(operandId)) return true;
            // Verificar sub-fragments dentro deste operand
            for (Object el : op.getElements()) {
                if (el instanceof Fragment && containsOperand((Fragment) el, operandId)) return true;
            }
        }
        return false;
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
