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
     * Constroi um objeto Interaction.
     * Faz:
     * 1 - parse das lifelines (para depois associar às mensagens)
     * 2 - parse das mensagens (criamos os objetos Message e criamos 2 maps,
     *   um de messageId -> Message e outro de messageId -> sendEvent, para depois construir a hierarquia dos fragments
     * 3 - construir a hierarquia de fragments
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

        // parsing das mensangens
        Map<String, ModelElement> sendEventsByMessageId = new HashMap<>();
        Map<String, Message> messagesById = parseMessages(element, interaction, sendEventsByMessageId);

        buildFromMessages(element, interaction, messagesById, sendEventsByMessageId);

        // context = "dono" do diagrama: o tipo do alvo da mensagem de entrada (actor -> lifeline).
        // distingue diagramas com o mesmo nome (ex: versão entity vs service)
        interaction.setContextClass(resolveContextClass(interaction));
        return interaction;
    }

    /**
     * O "dono" do diagrama ou seja o tipo do alvo da mensagem de entrada (actor -> lifeline),
     * ou seja a lifeline que o actor chama. como "fallback" a primeira lifeline com tipo.
     */
    private String resolveContextClass(Interaction interaction) {
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
     * Constroi a arvore de elementos (Messages e Fragments) do Interaction a partir das mensagens.
     * Usa a ordem do VP number para garantir que ordem "visual" é a certa
     *
     * Usa mapas auxiliares:
     * - messageId -> Message (para obter o Message a partir do id da mensagem)
     * - messageId -> sendEvent (para obter o sendEvent associado a cada mensagem)
     * - operandId -> Operand (para obter o Operand a partir do id do fragmento combinado)
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
            // o número tem de ser do tipo "1" ou "1.2.3"
            if (!vp.matches("\\d+(\\.\\d+)*")) {
                throw new IllegalArgumentException(
                        "Diagrama de sequência '" + interaction.getName() + "': a mensagem '"
                        + (m.getRawName() != null ? m.getRawName() : m.getName())
                        + "' tem um número de sequência inválido ('" + vp + "') — esperado algo como 1 ou 1.2.");
            }
        }

        // ordenar por VP number para garantir que a ordem visual é a correta
        sorted.sort((a, b) -> compareWithTheDot(a.getVpNumber(), b.getVpNumber()));

        // percorre as mensagems por ordem e coloca cada uma no seu "container"
        for (Message msg : sorted) {
            // entry message (actor -> lifeline): anexar direto à raiz
            if (msg.getSource() == null || msg.getSource().getType() == null) {
                interaction.getElements().add(msg);
                continue;
            }

            ModelElement sendEvent = sendEventsByMessageId.get(msg.getId());
            if (sendEvent == null) continue;

            // o "container" da mensagem é o owner do seu sendEvent
            ModelElement container = sendEvent.getOwner();

            // obtem a lista de elementos do container da mensagem, e adiciona
            List<Object> containerList = getOrCreateContainerElements(container, interaction, rootId, operands, fragments);
            containerList.add(msg);
        }
    }

    // devolve a lista de elementos do "container" cria operand/fragments pelo caminho acima

    /**
     * Dado um container, devolve a lista de elementos onde deve inserir
     *
     * Existem 3 casos
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
        if (id.equals(rootId)) return interaction.getElements();

        Operand op = operands.get(id);
        if (op != null) return op.getElements();

        // criar o operand
        op = new Operand();
        op.setId(id);
        ModelElement guardELeme = (ModelElement) container.getRefAttribute("guard");
        if (guardELeme != null) op.setGuard(guardELeme.getPlainAttribute("value"));
        operands.put(id, op);

        // anexar ao fragmento pai (criar se preciso e recursivo até a raiz)
        ModelElement combFragelem = container.getOwner();
        Fragment frag = fragments.get(combFragelem.getXMIID());
        if (frag == null) {
            frag = new Fragment();
            frag.setId(combFragelem.getXMIID());
            frag.setOperator(combFragelem.getPlainAttribute("operator"));
            fragments.put(combFragelem.getXMIID(), frag);
            List<Object> parentList = getOrCreateContainerElements(combFragelem.getOwner(), interaction, rootId, operands, fragments);
            parentList.add(frag);
        }
        frag.getOperands().add(op);
        return op.getElements();

    }


    private Map<String, Message> parseMessages(ModelElement element, Interaction interaction,
                                               Map<String, ModelElement> sendEventsByMessageId) {
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
            // represents é um ownedAttribute — o nome é o nome da variável ("u")
            String varName = represents.getName();
            lifeline.setRepresents(varName);

            // o tipo da lifeline está no VP extension do ownedAttribute (<type value="User"/>)
            String vpType = represents.getPlainAttribute("vptype");
            if (vpType != null && !vpType.isEmpty()) {
                lifeline.setType(vpType);
            }
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

        // sem parenteses
        if (parenOpen == -1) {
            if (message.getAssignVariable() != null) {
                // atribuição simples: "x = expr" ou "x = expr : Type" (ou seja nao seja uma chamada a um metodo)
                String expr = signaturePart.trim();
                int colon = findColonOutsideGenerics(expr);
                if (colon != -1) {
                    String type = expr.substring(colon + 1).trim();
                    if (!type.isEmpty()) message.setReturnType(type);   // : tipo -> declarar com este tipo
                    expr = expr.substring(0, colon).trim();
                }
                message.setAssignExpression(expr);
            } else {
                // nome simples sem argumentos
                message.setName(signaturePart.trim());
            }
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
     * Divide uma string por , mas ignora as vírgulas dentro de < > (listas/maps)
     * e dentro de ( ) — um argumento "criaAluno(nome, numero)" é UM argumento,
     * senão a heurística de coleções (size()==1) não dispara e gera acesso a campo private
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
     * Encontra o primeiro ':' que não esteja dentro de < > nem de ( )
     * (evita confundir com generics como List<User> ou com um ':' dentro de uma chamada aninhada)
     */
    private int findColonOutsideGenerics(String input) {
        int depth = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<' || c == '(') depth++;
            else if (c == '>' || c == ')') depth--;
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
