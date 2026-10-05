package parser;

import java.util.*;

import com.sdmetrics.model.*;

import model.sequence.*;

/**
 * coisas principais
 * interaction = diagrama de sequência
 * message = signature é a mensagem em si, sort é o tipo (sync etc) e send/receive events para associar às lifelines
 * sendEvent / receiveEvent = mensagem aponta para estes dois, de onde sai, onde chega
 */
public class SequenceDiagramParser {
    private Model model;
    private MetaModel metaModel;
    //  messageId -> num vp
    private Map<String, String> messageNumbers;

    public SequenceDiagramParser(Model model, MetaModel metaModel, String xmiFile) throws java.io.IOException {
        this.model = model;
        this.metaModel = metaModel;
        // extrai os numeros das mensagens do VP (extension "vpnumber" do Message)
        // é a unica maneira de obter ordem correta
        this.messageNumbers = VpParser.extractMessageNumbers(xmiFile);
    }

    // pega nas "interacations" (diagramas de sequencia) do modelo
    public List<Interaction> parseInteractions() {
        List<Interaction> interactions = new ArrayList<>();

        List<ModelElement> interactionElements = model.getAcceptedElements(
                metaModel.getType("interaction")
        );

        for (ModelElement element : interactionElements) {
            Interaction interaction = parseInteraction(element);
            if (interaction != null) {
                interactions.add(interaction);
            }
        }

        return interactions;
    }

    /**
     * faz um objeto Interaction que basicamente é o diagrama.
     * 1 - faz parse das lifelines (para depois associar às mensagens)
     * 2 - depos parse das mensagens (criamos os objetos Message e criamos 2 maps,
     *   um de messageId -> Message e outro de
     *   messageId -> sendEvent, para depois construir a hierarquia dos fragments
     * 3 - construir a a interaction em si
     */
    private Interaction parseInteraction(ModelElement element) {
        Interaction interaction = new Interaction();
        interaction.setId(element.getXMIID());
        interaction.setName(element.getName());

        // obter as lifelines e o seu parsing
        Collection<?> lifelines = element.getSetAttribute("lifelines");
        if (lifelines != null) {
            for (Object o : lifelines) {
                interaction.getLifelines().add(parseLifeline((ModelElement) o));
            }
        }

        // parsing das mensangens
        Map<String, ModelElement> sendEventsByMessageId = new HashMap<>();
        Map<String, Message> messagesById = parseMessages(element, interaction, sendEventsByMessageId);

        buildFromMessages(element, interaction, messagesById, sendEventsByMessageId);

        // "dono" do diagrama o tipo do alvo da mensagem de entrada (actor -> lifeline).
        // distingue diagramas com o mesmo nome, mais para displayt na frontend
        interaction.setContextClass(firstLifeline(interaction));
        return interaction;
    }

    /**
     * o "dono" do diagrama ou seja o tipo do alvo da mensagem de entrada (actor -> lifeline),
     * ou seja a lifeline que o actor chama. como "fallback" a primeira lifeline com tipo.
     */
    private String firstLifeline(Interaction interaction) {
        for (Object el : interaction.getElements()) {
            if (el instanceof Message m
                    && (m.getSource() == null || m.getSource().getType() == null)
                    && m.getTarget() != null && m.getTarget().getType() != null) {
                return m.getTarget().getType();
            }
        }
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() != null) return ll.getType();
        }
        return null;
    }


    /**
     * Constroi a estrutura de elementos (Messages e Fragments) do Interaction a partir das mensagens
     * temos de usar a ordem do VP number para garantir que ordem "visual" é a certa
     *
     *  mapas helpers:
     * - messageId -> Message (para obter o Message a partir do id da mensagem)
     * - messageId -> sendEvent (para obter o sendEvent associado a cada mensagem)
     * - operandId -> Operand (para obter o Operand a partir do id do operand)
     * - fragmentId -> Fragment (para obter o Fragment a partir do id do fragmento
     *
     * Para cada mensagem ele pega no sendEvent e ve quem é owner, isso indica o "container" da mensagem
     * esse container pode ser a propria interação (root) ou um operand, (mensagem dentro de um loop etc)
     *
     * Com o container da mensagem, ele chama getOrCreateContainerElements que é recursiva e sobe a arvore de fragments até chegar à raiz (interação)
     */

    private void buildFromMessages(ModelElement interactionElement, Interaction interaction,
                                   Map<String, Message> messagesById,
                                   Map<String, ModelElement> sendEventsByMessageId) {
        Map<String, Operand> operands = new HashMap<>();
        Map<String, Fragment> fragments = new HashMap<>();
        String rootId = interactionElement.getXMIID();

        // mensagens ordenadas por VP number
        List<Message> sorted = new ArrayList<>(messagesById.values());

        // cada mensagem precisa de número de sequência para podermos ordenar o corpo
        // isto nunca deve dar erro porque TEM de vir do visual paradigm, mas para ter a certeza
        for (Message m : sorted) {
            String vp = m.getVpNumber();
            if (vp == null || vp.isBlank()) {
                throw new IllegalArgumentException(
                        "Diagrama de sequência '" + interaction.getName() + "': a mensagem '"
                        + (m.getRawName() != null ? m.getRawName() : m.getName())
                        + "' não tem número de sequência — não é possível determinar a ordem.");
            }
            // vale a pena validar se o vp number é válido ??
        }

        // ordenar por VP number para garantir que a ordem visual é a correta
        sorted.sort((a, b) -> compareWithTheDot(a.getVpNumber(), b.getVpNumber()));

        // percorre as mensagems por ordem e colocar cada uma no seu "container"
        for (Message msg : sorted) {
            // entry message (actor -> lifeline) metemos direto à raiz
            if (msg.getSource() == null || msg.getSource().getType() == null) {
                interaction.getElements().add(msg);
                continue;
            }

            ModelElement sendEvent = sendEventsByMessageId.get(msg.getId());
            if (sendEvent == null) continue;

            // o "container" da mensagem é o owner do seu sendEvent
            ModelElement container = sendEvent.getOwner();

            // containerElements = referência à lista de filhos do container onde se vai meter esta mensagem
            //   raiz               -> interaction.getElements()
            //   dentro de fragment -> operand.getElements()
            List<Object> containerElements = getOrCreateContainerElements(container, interaction, rootId, operands, fragments);
            // inserir a mensagem no fim da lista de elementos deste cont
            containerElements.add(msg);
        }
    }

    // devolve a lista de elementos do "container" cria operand/fragments pelo caminho acima

    /**
     * Dado um container, devolve a lista de elementos onde deve inserir
     *
     * 3 casos
     * 1 - o container é a própria interação (root) -> devolve interaction.getElements()
     *
     * 2 - o container é um operand já criado -> devolve operand.getElements()
     *
     * 3 - o operand ainda nao existe, quer dizer que precisa de ser construido
     *     cria entao o operand, e regista-o no mapa respetivo
     *     Depois trata do seu CombinedFragment pai:
     *      - se ja existir em fragments, apenas adiciona o novo operand a lista desse fragment
     *      - não existe -> cria-o e chama-se recursivamente sobre o owner do fragment
     *        para descobrir ONDE inseri-lo. A recursão sobe o nesting, criando os pais
     *        que faltam, até à raiz (ou a um nó já criado).
     */
    private List<Object> getOrCreateContainerElements(ModelElement container, Interaction interaction, String rootId,
                                              Map<String, Operand> operands, Map<String, Fragment> fragments) {
        String id = container.getXMIID();
        // container é a propria interação (root
        if (id.equals(rootId)) return interaction.getElements();

        Operand op = operands.get(id);
        // operand já construído por uma mensagem anterior
        if (op != null) return op.getElements();

        // criar o operand
        op = new Operand();
        op.setId(id);
        // guard = condição do branch
        ModelElement guardELeme = (ModelElement) container.getRefAttribute("guard");
        if (guardELeme != null) op.setGuard(guardELeme.getPlainAttribute("value"));
        operands.put(id, op);

        // anexar ao fragmento pai (criar se preciso e recursivo até a raiz)

        // liga o operand ao seu CombinedFragment (o pai).
        // se o fragment ainda não exist cria-o e liga o PRÓPRIO fragment ao dono
        // dele recursivamente
        // sobe a cadeia de pais até à raiz ou a um nó já criado.

        // operandos pertencem a CombinedFragments (os pais)
        ModelElement combFragelem = container.getOwner();
        Fragment frag = fragments.get(combFragelem.getXMIID());
        if (frag == null) {
            // nao existe entao tem de ser criado
            frag = new Fragment();
            frag.setId(combFragelem.getXMIID());
            frag.setOperator(combFragelem.getPlainAttribute("operator"));
            fragments.put(combFragelem.getXMIID(), frag);
            // liga o fragmento ao seu pai, cria recursivamente se precisar
            List<Object> parentList = getOrCreateContainerElements(combFragelem.getOwner(), interaction, rootId, operands, fragments);
            parentList.add(frag);
        }
        // por fim, liga este operando ao fragmento
        frag.getOperands().add(op);
        return op.getElements();

    }


    private Map<String, Message> parseMessages(ModelElement element, Interaction interaction,
                                               Map<String, ModelElement> sendEventsByMessageId) {

        // mapa messageId -> Message
        Map<String, Message> messagesById = new HashMap<>();
        Collection<?> messages = element.getSetAttribute("messages");
        if (messages == null) return messagesById;

        for (Object o : messages) {
            ModelElement msgEl = (ModelElement) o;
            Message msg = parseMessage(msgEl, interaction);
            if (msg == null) continue;
            messagesById.put(msg.getId(), msg);

            ModelElement sendEvt = (ModelElement) msgEl.getRefAttribute("sendevent");
            if (sendEvt != null) sendEventsByMessageId.put(msg.getId(), sendEvt);
        }
        return messagesById;
    }

    private Lifeline parseLifeline(ModelElement element) {
        Lifeline lifeline = new Lifeline();

        lifeline.setId(element.getXMIID());
        lifeline.setName(element.getName());


        ModelElement represents = (ModelElement) element.getRefAttribute("represents");
        if (represents != null) {
            // represents é um ownedAttribute e o nome é o nome da variável ("u")
            String varName = represents.getName();
            lifeline.setRepresents(varName);

            // o tipo da lifeline está no VP extension do ownedAttribute (<type value="User"/>)
            String vpType = represents.getPlainAttribute("vptype");
            if (vpType != null && !vpType.isEmpty()) {
                lifeline.setType(vpType);
            }
        }
        // fallback se nao conseguir a meter a dar com vp
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

        return lifeline;
    }

    // parse messages
    private Message parseMessage(ModelElement element, Interaction interaction) {
        Message message = new Message();

        message.setId(element.getXMIID());
        message.setVpNumber(messageNumbers.get(element.getXMIID()));

        // nome tem tudo (func(arg1 : type1) : returntype)
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

        return message;
    }

    /**
     * parsing das mensages pelo "raw name" que pode conter nome, argumentos e return type
     *   getTurma()
     *   setTurma(novaTurma: Turma)
     *   get(users: ArrayList<User>) : User
     *   var = resto
     */
    private void parseMessageSignature(Message message, String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            message.setName(rawName);
            return;
        }

        // verificar se tem assignment (var = method)
        String signaturePart = rawName;
        int equalsIndex = rawName.indexOf('=');
        // paren = chamada de metodo
        int parenOpen = rawName.indexOf('(');

        // 1
        // primeiro separa a atribuição se houver var = ...
        // o '=' tem de vir antes do '(' para ser assignment (senão pode ser parte de um argumento)
        if (equalsIndex != -1 && (parenOpen == -1 || equalsIndex < parenOpen)) {
            // var de atribuição , tudo antes do '='
            message.setAssignVariable(rawName.substring(0, equalsIndex).trim());
            // resto da assinatura (method(args) : returnType), tudo depois do '='
            signaturePart = rawName.substring(equalsIndex + 1).trim();
            parenOpen = signaturePart.indexOf('(');
        }

        // 2
        // sem parenteses entao não é chamada, é atribuiçãp
        if (parenOpen == -1) {
            // se tiver assignVariable, então é "x = expr" ou "x = expr : Type"
            if (message.getAssignVariable() != null) {
                String expr = signaturePart.trim();
                // encontra onde está o :
                int colon = findTypeSeparator(expr);
                // tem tipo
                if (colon != -1) {
                    // pega no tipo
                    String type = expr.substring(colon + 1).trim();
                    if (!type.isEmpty()) message.setReturnType(type);
                    // o resto é a expressão ex "getUser()"
                    expr = expr.substring(0, colon).trim();
                }
                // é so um assign
                message.setAssignExpression(expr);
            } else {
                // sem assign variable é so instaciar uma var
                // ex "varName: Type" ou "varName"
                int colon = findTypeSeparator(signaturePart);
                if (colon != -1) {
                    // var name ta antes do colon, tipo de pois
                    String varName = signaturePart.substring(0, colon).trim();
                    String type    = signaturePart.substring(colon + 1).trim();
                    if (!varName.isEmpty() && !type.isEmpty()) {
                        message.setAssignVariable(varName);
                        message.setReturnType(type);   // assignExpression fica null -> declaração sem valor
                        return;
                    }
                }
                // nome simples sem argumentos
                message.setName(signaturePart.trim());
            }
            return;
        }

        // 3
        // se chegar aqui é porque é chamada de um metodo

        // nome do metodo é tudo antes do '('
        message.setName(signaturePart.substring(0, parenOpen).trim());

        int parenClose = signaturePart.lastIndexOf(')'); // nao foi fechado ent ignora
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
        int colonIndex = findTypeSeparator(argString);

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
     * divide a string por , mas ignora as vírgulas dentro de < > (maps)
     * e dentro de ( ) — um argumento "criaAluno(nome, numero)" é UM argumento,
     * senão a heurística de coleções (size()==1) não funcionava e gera acesso a campo private
     */
    private List<String> splitArguments(String input) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<' || c == '(') depth++;
            else if (c == '>' || c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(input.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(input.substring(start));

        return parts;
    }

    /**
     * find o primeiro ':' que não esteja dentro de < > nem de ( )
     * (evita confundir com generics como List<User> ou com um ':' dentro de uma chamada aninhada)
     */
    private int findTypeSeparator(String input) {
        int depth = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<' || c == '(') depth++; // ta num generic ou parênteses
            else if (c == '>' || c == ')') depth--; // saiu
            else if (c == ':' && depth == 0) return i; // // ':'
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
    
    // comprar tendo em conta os numeros do vp que sao 1 1.1 1.1.1 1.2...
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
