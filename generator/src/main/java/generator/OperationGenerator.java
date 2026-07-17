package generator;

import model.Association;
import model.Class;
import model.Operation;
import model.annotations.Postcondition;
import model.annotations.PreSnapshot;
import model.annotations.Precondition;
import model.sequence.*;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * ========
 * OperationGenerator
 * ==========
 *
 * VISÃO GERAL
 *   Uma operação (ex.: Turma.inscrever(a: Aluno)) não tem corpo no diagrama de classes — só assinatura.
 *   Para gerar um corpo, o utilizador desenha um DIAGRAMA DE SEQUÊNCIA para a operação, e este gerador
 *   transcreve-o para Java. É o único sítio onde o COMPORTAMENTO UML (não só a estrutura) vira código;
 *   é partilhado por plain/CLI/CLI-JPA (e alimenta o Spring via SequenceBodyGenerator).
 *   Modelo mental: o corpo do metodo = as mensagens que o DONO da operação envia, por ordem.
 *
 * REGRAS PARA DESENHAR O DIAGRAMA (contrato assumido; a maioria é validada com erro claro)
 *   - Um actor (lifeline sem tipo) chama a operação; essa 1ª mensagem é a "entry message".
 *   - O nome da entry message === nome da operação.
 *   - Todos os argumentos e o return são tipados explicitamente (a: Aluno; ... : boolean).
 *   - O nome de uma lifeline === o parâmetro/variável que representa (para chamadas cross-lifeline).
 *   - As lifelines do dono são tipadas (turma: Turma).
 *   - Uma reply para o actor dá o valor de retorno; sem reply => void.
 *   - Mensagens «create» sem argumentos; pôr os valores com setters a seguir.
 *
 * PASSO A PASSO — generateOperation
 *   1. Encontra a entry message (primeira mensagem, actor->dono); se não há -> erro.
 *   2. Valida que o nome da entry === nome da operação.
 *   3. Resolve o tipo de retorno (entry -> class diagram -> void).
 *   4. Emite a assinatura a partir dos ARGUMENTOS DA ENTRY (não do class diagram); cada arg tem de ter tipo.
 *   5. Precondições OCL -> if (!(expr)) throw IllegalStateException(msg).
 *   6. Snapshots @pre (para pós-condições que referem x@pre) declarados ANTES do corpo.
 *   7. Corpo -> generateElements.
 *   8. Expressão de retorno -> findReplyToActor (última reply para o actor); non-void sem reply -> erro.
 *   9. Alias "result" (se uma pós-condição o referir) declarado DEPOIS do corpo.
 *   10. Pós-condições OCL -> mesma forma if (!(...)) throw.
 *   11. return result / return <expr>.
 *
 * generateElements — O FILTRO DE POSSE (decisão central)
 *   Só emite uma Message se a sua source for o DONO (entry.getTarget()) e não for reply. Fragments recorrem.
 *   Consequência: o corpo é só as chamadas DIRETAS do dono. Se o dono chama a.foo() e a chama b.bar(),
 *   a mensagem b.bar() (source = a) é IGNORADA. Transcrição UM NÍVEL DE PROFUNDIDADE, por design — os
 *   callees têm as suas próprias operações/diagramas.
 *
 * generateMessage — os cinco casos (por ordem de prioridade)
 *   1. «create» -> Type var = new Type(args);
 *   2. atribuição simples (isPlainAssignment, ex.: points = points - amount) -> [Type] x = expr;
 *   3. receiver: "this" se o target é o próprio dono, senão o nome da lifeline (= nome de um parâmetro).
 *   4. heurística add/remove: <campo>.add(x)/.remove(x) (ou add/remove com 1 arg cujo tipo casa numa
 *      associação) -> acesso à coleção (qualifier/Map: put/remove por chave; senão add/remove).
 *   5. fallback: chamada literal receiver.name(args), com atribuição se houver assign var.
 *
 * generateFragment — estruturas de controlo (pelo guard do operando)
 *   - loop com guard "for (Tipo v : coll)" -> for-each Java; "while (cond)" -> while; outro -> erro.
 *     Coleções Map levam .values() (adaptCollectionAccess).
 *   - opt -> if (guard) { }.
 *   - alt -> if / else if / else pelos guards dos operandos (o 1º tem de ter guard; "else" -> else final).
 *   Fragments aninhados recorrem via generateElements com indent+1.
 *
 * CAMADA OCL
 *   Pré/pós-condições -> if (!(javaExpr)) throw IllegalStateException(msg). @pre precisa de um snapshot
 *   ANTES do corpo; "result" precisa de um alias DEPOIS do corpo (por isso um vai acima e o outro abaixo).
 *
 * HELPERS
 *   - findEntryMessage/firstMessage — primeira mensagem, descendo em fragments.
 *   - participates — ownerType é (ou herda de) a classe desta ponta da associação (subclasse reconhece
 *     associação do pai).
 *   - findAssoc/findAssocByField/fieldNameFor — resolvem a que associação um add/remove se refere e o campo.
 *   - adaptCollectionAccess — acrescenta .values() ao iterar uma associação Map (qualificada).
 */
/**
 * Pega numa interação (diagrama de sequência) ligada a uma operação de uma classe
 * e gera o corpo do metodo correspondente.
 *
 * processo:
 *   1. generateOperation — cria metodo, emite preconditions, @pre snapshots,
 *      corpo (via generateElements), result alias, postconditions, return
 *   2. generateElements — itera mensagens e fragmentos, só processa mensagens enviadas pelo DONO da
 *      operação (UM nível de profundidade, por design): o que os callees fazem em resposta NÃO é
 *      inlined — pertence às operações deles. (Por isso um fragmento sem ação do dono sai vazio.)
 *   3. generateMessage — gera código para uma mensagem (heurística + fallback)
 *   4. generateFragment — gera código para loop / alt / opt (recursivo)
 *
 * Heurísticas:
 *   - target == entry.target -> "this", senão usa o nome da lifeline
 *   - <attr>.add(x) ou <attr>.remove(x) -> put/remove para Map (com qualifier),
 *     senão add/remove para coleção simples
 *   - loops sobre Map -> adaptados para iterar .values()
 *   - reply para o actor -> return statement, sem reply -> sem return (void)
 *
 * OCL:
 *   - preconditions e postconditions são emitidas como if (!(...)) throw IllegalStateException
 *   - @pre nas postconditions -> snapshot do estado declarado antes do corpo
 *   - "result" nas postconditions -> alias declarado depois do corpo, antes das checks
 *
 * Convenções ao utilizador fazer o diagrama:
 *   - Tipos explícitos em todos os argumentos e returns (a: Aluno, não apenas a)
 *   - Actor (lifeline sem tipo) chama a operação ( a nao ser que seja job )
 *   - Nome da mensagem do actor === nome da operação
 *   - Nome da lifeline === nome do parâmetro (para cross-lifeline calls)
 *   - Lifelines do owner devem ser tipadas (turma:Turma)
 *   - Mensagens «create» SEM argumentos (new Type()); pôr os valores com mensagens setter a seguir
 *
 *
 *   TODO
 *   - POST CONDS REVERTER NO CLI/PLAIN
 */

public class OperationGenerator {

    // gera a operação PIM, assinatura, precondições e corpo
    public static void generateOperation(PrintWriter writer, Class cls, Operation operation,
                                         Interaction interaction, List<Association> associations) {
        Message entry = findEntryMessage(interaction);

        if (entry == null) {
            throw new IllegalArgumentException("Operação '" + operation.getName()
                    + "': o diagrama não tem mensagem de entrada (actor -> " + cls.getName()
                    + ") com o nome da operação.");
        }

        // a mensagem de entrada tem de corresponder à operação declarada (pelo nome)
        if (entry.getName() != null && !entry.getName().equals(operation.getName())) {
            throw new IllegalArgumentException(
                    "Diagrama da operação '" + operation.getName() + "': a mensagem de entrada '"
                    + entry.getName() + "' não corresponde ao nome da operação.");
        }

        // tipo de retorno da mensagem de entrada se especificado | senão da operação (diagrama de classes) | senão void
        String returnType = (entry.getReturnType() != null && !entry.getReturnType().isBlank())
                ? entry.getReturnType()
                : (operation.getReturnType() != null ? operation.getReturnType() : "void");

        // assinatura a partir da mensagem de entrada o tipo de cada argumento é obrigatório
        writer.println();
        writer.print("    public " + returnType + " " + entry.getName() + "(");
        for (int i = 0; i < entry.getArguments().size(); i++) {
            if (i > 0) writer.print(", ");
            MessageArgument arg = entry.getArguments().get(i);
            if (arg.getType() == null || arg.getType().isBlank()) {
                throw new IllegalArgumentException(
                        "Diagrama da operação '" + operation.getName() + "': o argumento '"
                        + arg.getName() + "' não tem tipo (ex: '" + arg.getName() + " : Tipo').");
            }
            writer.print(GeneratorUtils.removePackageNames(arg.getType()) + " " + arg.getName());
        }
        writer.println(") {");

        for (Precondition pre : operation.getPreconditions()){
            writer.println("        if (!(" + pre.getJavaExpression() + ")) {");
            writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(pre.getMessage()) + "\");");
            writer.println("        }");
        }

        // @pre snapshots - capturar o estado antes de executar
        // dedup por varName: várias postconditions na mesma op podem mencionar o mesmo @pre
        Map<String, PreSnapshot> snapshots = new LinkedHashMap<>();
        for (Postcondition post : operation.getPostconditions()) {
            for (PreSnapshot snap : post.getPreSnapshots()) {
                snapshots.putIfAbsent(snap.getVarName(), snap);
            }
        }
        for (PreSnapshot snap : snapshots.values()) {
            writer.println("        " + snap.getJavaType() + " " + snap.getVarName()
                    + " = " + snap.getJavaInit() + ";");
        }

        generateElements(writer, interaction.getElements(), entry, cls, associations, 2);

        // a expressão de return vem da reply-message para o actor;
        // sem reply, não é feito return
        String returnExpr = GeneratorUtils.findReplyToActor(interaction.getElements());

        // OCL postcondition pode referir "result" — declaramos alias se algum a usar
        boolean needsResult = operation.getPostconditions().stream()
                .anyMatch(p -> p.getJavaExpression() != null
                        && p.getJavaExpression().matches(".*\\bresult\\b.*"));

        if (!"void".equals(returnType) && returnExpr == null) {
            throw new IllegalArgumentException(
                    "Operação '" + operation.getName() + "' devolve '" + returnType
                    + "' mas o diagrama de sequência não tem mensagem de resposta (reply) para o ator.");
        }
        if (needsResult && returnExpr == null) {
            throw new IllegalArgumentException(
                    "Operação '" + operation.getName() + "': uma poscondição refere 'result' "
                    + "mas o diagrama de sequência não tem mensagem de resposta (reply) para o ator.");
        }
        if (needsResult && "void".equals(returnType)) {
            throw new IllegalArgumentException(
                    "Operação '" + operation.getName() + "': uma pós-condição refere 'result' "
                    + "mas a operação é void (não tem valor de retorno).");
        }

        if (needsResult && returnExpr != null && !"void".equals(returnType)) {
            writer.println("        " + returnType + " result = " + returnExpr + ";");
        }

        for (Postcondition post : operation.getPostconditions()) {
            writer.println("        if (!(" + post.getJavaExpression() + ")) {");
            writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(post.getMessage()) + "\");");
            writer.println("        }");
        }

        if (!"void".equals(returnType) && returnExpr != null) {
            writer.println("        return " + (needsResult ? "result" : returnExpr) + ";");
        }

        writer.println("    }");
    }

    // Itera as messages e fragments, delegando para generateMessage / generateFragment
    // so emite mensages enviadas pelo dono do metodo o resto ignora, o resto seria outro diagrama
    private static void generateElements(PrintWriter writer, List<Object> elements,
                                         Message entry, Class cls, List<Association> associations, int indent) {
        String indentStr = "    ".repeat(indent);
        for (Object el : elements) {
            if (el instanceof Message) {
                Message msg = (Message) el;
                if (msg.getSource() == null || msg.getSource().getType() == null) continue;
                if (!msg.getSource().getId().equals(entry.getTarget().getId())) continue;
                if ("reply".equals(msg.getMessageSort())) continue; // replies só serverm para o return — tratadas em findReplyToActor
                generateMessage(writer, msg, entry, cls, associations, indentStr);
            } else if (el instanceof Fragment) {
                generateFragment(writer, (Fragment) el, entry, cls, associations, indent);
            }
        }
    }

    /**
     * Gera o codiog da mensagem
     * - target igual ao entry -> "this"
     *   nome da lifeline se tiver, ou comentário se não tiver
     * - add / remove com 1 argumento e ascociação correspondente -> acesso à coleção (add/remove ou put/remove para map)
     * - caso geral: receiver.name(args) com assign se tiver variável de atribuição e tipo de retorno
     */
    private static void generateMessage(PrintWriter writer, Message msg, Message entry,
                                        Class cls, List<Association> associations, String indent) {
        // mensagem «create»: gera "Type var = new Type(args)".
        // Antes de isPlainAssignment porque "loan =" parece atribuição
        // Só compila se Type tiver construtor público com esses args (em Spring só no-arg) — nota #9 no Main.
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            String var = msg.getAssignVariable() != null ? msg.getAssignVariable()
                    : (msg.getTarget().getRepresents() != null && !msg.getTarget().getRepresents().isBlank()
                        ? msg.getTarget().getRepresents()
                        : GeneratorUtils.lowercaseFirst(type));
            StringBuilder a = new StringBuilder();
            if (msg.getArguments() != null) {
                for (int i = 0; i < msg.getArguments().size(); i++) {
                    if (i > 0) a.append(", ");
                    a.append(msg.getArguments().get(i).getName());
                }
            }
            writer.println(indent + type + " " + var + " = new " + type + "(" + a + ");");
            return;
        }

        // atribuição simples (quando nao tem chamadas a metodos): "x = expr" ou "x = expr : Type"
        // se ": Type" presente -> declara (Type x = expr); ausente -> reatribui (x = expr)
        if (msg.isPlainAssignment()) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            return;
        }

        String receiver;
        if (msg.getTarget().getId().equals(entry.getTarget().getId())) {
            receiver = "this";
        } else {
            receiver = msg.getTarget().getName();
            if (receiver == null || receiver.isEmpty()) {
                throw new IllegalArgumentException(
                        "Diagrama de sequência: a lifeline do tipo '" + msg.getTarget().getType()
                        + "' não tem nome — o nome tem de ser igual ao parâmetro/variável que está a ser chamado.");
            }
        }
        // heurística: <xx>.add(x) / <xx>.remove(x) ou addX/removeX -> acesso a coleção
        String name = msg.getName();
        List<MessageArgument> args = msg.getArguments();

        // "alunos.add(a)" -> attrName="alunos", op="add"
        String attrName = null;
        String op = name;
        int sep = name.indexOf(".");
        if (sep > 0) {
            attrName = name.substring(0, sep);
            op = name.substring(sep + 1);
        }

        if (args != null && args.size() == 1 && ("add".equals(op) || "remove".equals(op))) {
            MessageArgument arg = args.get(0);
            String ownerType = msg.getTarget().getType();

            // lookup direto por attrName
            Association assoc;
            if (attrName != null) {
                assoc = findAssocByField(associations, cls, ownerType, attrName);
            } else {
                assoc = findAssoc(associations, cls, ownerType, arg.getType());
                if (assoc != null) attrName = fieldNameFor(assoc, cls, ownerType);
            }

            if (assoc != null) {
                String base = receiver + "." + attrName;
                String code;
                if (assoc.hasQualifier()) {
                    // MAP com PUT(key, value) ou REMOVE(key) — key via getter do qualifier
                    String q = assoc.getQualifierName();
                    String getter = "get" + Character.toUpperCase(q.charAt(0)) + q.substring(1) + "()";
                    code = "add".equals(op)
                            ? base + ".put(" + arg.getName() + "." + getter + ", " + arg.getName() + ");"
                            : base + ".remove(" + arg.getName() + "." + getter + ");";
                } else {
                    // coleção simples: ADD/REMOVE
                    code = base + "." + op + "(" + arg.getName() + ");";
                }
                writer.println(indent + code);
                return;
            }
        }

        // fallback: chamada literal receiver.name(args)
        StringBuilder call = new StringBuilder();
        call.append(receiver).append(".").append(name).append("(");
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) call.append(", ");
                call.append(args.get(i).getName());
            }
        }
        call.append(")");

        if (msg.getAssignVariable() != null) {
            // se ": Type" presente -> declara; ausente -> reatribui a variável porque já tem de ser existente
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + call + ";");
        } else {
            writer.println(indent + call + ";");
        }
    }

    /**
     * VERSÃO ALTERNATIVA (mais literal) de generateMessage — NÃO ligada ainda.
     * Igual à original, mas a heurística add/remove reduz-se ao ADAPTADOR DE MAP: só um campo
     * QUALIFICADO (<campo>.add/.remove) vira put/remove por chave. Listas planas e chamadas normais
     * caem no fallback (transcrição literal); um "add(x)" SEM campo passa a ser chamada a método.
     * Muda o contrato de modelação: diagramas com "add(x)" por match de tipo (ex.: add(aluno : Aluno))
     * partiriam — teriam de passar a "alunos.add(a)". Ver nota no Main. Decidir com orientador.
     */
    private static void generateMessage2(PrintWriter writer, Message msg, Message entry,
                                        Class cls, List<Association> associations, String indent) {
        // mensagem «create»: gera "Type var = new Type(args)".
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            String var = msg.getAssignVariable() != null ? msg.getAssignVariable()
                    : (msg.getTarget().getRepresents() != null && !msg.getTarget().getRepresents().isBlank()
                        ? msg.getTarget().getRepresents()
                        : GeneratorUtils.lowercaseFirst(type));
            StringBuilder a = new StringBuilder();
            if (msg.getArguments() != null) {
                for (int i = 0; i < msg.getArguments().size(); i++) {
                    if (i > 0) a.append(", ");
                    a.append(msg.getArguments().get(i).getName());
                }
            }
            writer.println(indent + type + " " + var + " = new " + type + "(" + a + ");");
            return;
        }

        // atribuição simples: "x = expr" ou "x = expr : Type"
        if (msg.isPlainAssignment()) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            return;
        }

        String receiver;
        if (msg.getTarget().getId().equals(entry.getTarget().getId())) {
            receiver = "this";
        } else {
            receiver = msg.getTarget().getName();
            if (receiver == null || receiver.isEmpty()) {
                throw new IllegalArgumentException(
                        "Diagrama de sequência: a lifeline do tipo '" + msg.getTarget().getType()
                        + "' não tem nome — o nome tem de ser igual ao parâmetro/variável que está a ser chamado.");
            }
        }

        String name = msg.getName();
        List<MessageArgument> args = msg.getArguments();

        // "alunos.add(a)" -> attrName="alunos", op="add"
        String attrName = null;
        String op = name;
        int sep = name.indexOf(".");
        if (sep > 0) {
            attrName = name.substring(0, sep);
            op = name.substring(sep + 1);
        }

        // só o caso Map: <campo>.add(x)/.remove(x) sobre um campo QUALIFICADO -> put/remove por chave.
        // listas planas e chamadas normais caem no fallback (transcrição literal).
        if (args != null && args.size() == 1 && attrName != null
                && ("add".equals(op) || "remove".equals(op))) {
            Association assoc = findAssocByField(associations, cls, msg.getTarget().getType(), attrName);
            if (assoc != null && assoc.hasQualifier()) {
                MessageArgument arg = args.get(0);
                String q = assoc.getQualifierName();
                String getter = "get" + Character.toUpperCase(q.charAt(0)) + q.substring(1) + "()";
                String base = receiver + "." + attrName;
                writer.println(indent + ("add".equals(op)
                        ? base + ".put(" + arg.getName() + "." + getter + ", " + arg.getName() + ");"
                        : base + ".remove(" + arg.getName() + "." + getter + ");"));
                return;
            }
            // não-Map -> cai no fallback
        }

        // fallback: chamada literal receiver.name(args)
        StringBuilder call = new StringBuilder();
        call.append(receiver).append(".").append(name).append("(");
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) call.append(", ");
                call.append(args.get(i).getName());
            }
        }
        call.append(")");

        if (msg.getAssignVariable() != null) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + call + ";");
        } else {
            writer.println(indent + call + ";");
        }
    }

    /**
     * fragments para estruturas Java:
     *  - loop com guard "x: coll"  -> for (x : coll)
     *  - loop sem iterador         -> while (guard)
     *  - opt                       -> if (guard)
     *  - alt                       -> if / else if / else conforme os guards dos operandos
     */
    private static void generateFragment(PrintWriter writer, Fragment frag, Message entry,
                                         Class cls, List<Association> associations, int indent) {
        String indentStr = "    ".repeat(indent);
        String op = frag.getOperator();

        // fragmento sem mensagens do dono -> bloco vazio (for/if {}); é modelação, não bug
        if ("loop".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard() != null ? operand.getGuard().trim() : "";

            // o guard escreve o loop: "for (Type var : toiter)" ou "while (cond)"
            if (guard.startsWith("for")) {
                String inner = GeneratorUtils.extractParen(guard);                 // "Aluno a : alunos"
                int colon = inner.indexOf(':');
                if (colon == -1) {
                    throw new IllegalArgumentException(
                            "Guard de loop 'for' tem de ser 'for (Tipo var : coleção)': '" + guard + "'");
                }
                String left = inner.substring(0, colon).trim();      // "Aluno a"
                String collection = inner.substring(colon + 1).trim().replace("self.", "this.");
                collection = adaptCollectionAccess(collection, cls, associations);   // Map -> .values()
                writer.println(indentStr + "for (" + left + " : " + collection + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
                writer.println(indentStr + "}");
            } else if (guard.startsWith("while")) {
                writer.println(indentStr + "while (" + GeneratorUtils.extractParen(guard) + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
                writer.println(indentStr + "}");
            } else {
                throw new IllegalArgumentException(
                        "Guard de loop tem de ser 'for (...)' ou 'while (...)': '" + guard + "'");
            }
        } else if ("opt".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard();
            if (guard == null || guard.isBlank()) {
                throw new IllegalArgumentException("Fragmento 'opt' tem de ter uma condição (guarda).");
            }
            writer.println(indentStr + "if (" + guard + ") {");
            generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
            writer.println(indentStr + "}");
        } else if ("alt".equals(op)) {
            List<Operand> ops = frag.getOperands();
            for (int i = 0; i < ops.size(); i++) {
                Operand operand = ops.get(i);
                String guard = operand.getGuard();
                if (i == 0) {
                    if (guard == null || guard.isBlank()) {
                        throw new IllegalArgumentException(
                                "Operação '" + cls.getName() + "': a primeira ramificação de um 'alt' tem de ter uma condição.");
                    }
                    writer.println(indentStr + "if (" + guard + ") {");
                } else if (guard != null && !guard.isEmpty() && !guard.equalsIgnoreCase("else")) {
                    writer.println(indentStr + "} else if (" + guard + ") {");
                } else {
                    writer.println(indentStr + "} else {");
                }
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
            }
            writer.println(indentStr + "}");
        }
    }

    // se a coleção é uma associação (Map), adiciona .values()
    // aceita "this.alunos" ou só "alunos"
    private static String adaptCollectionAccess(String expr, Class cls, List<Association> associations) {
        String fieldName = expr.startsWith("this.") ? expr.substring("this.".length()).trim() : expr.trim();

        for (Association assoc : associations) {
            // isOrInheritsFrom (não igual) porque a associação pode ser herdada de um pai
            boolean sourceSide = cls.isOrInheritsFrom(assoc.getSourceClass())
                    && assoc.isTargetNavigable()
                    && fieldName.equals(GeneratorUtils.makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity()));
            boolean targetSide = cls.isOrInheritsFrom(assoc.getTargetClass())
                    && assoc.isSourceNavigable()
                    && fieldName.equals(GeneratorUtils.makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity()));

            if ((sourceSide || targetSide) && assoc.hasQualifier()) {
                return expr + ".values()";
            }
        }
        return expr;
    }

    // ownerType é (ou herda de) a classe desta ponta da associação?
    // serve para uma subclasse reconhecer associações declaradas no pai.
    // tenta match direto pelo nomem, senão, se ownerType for a classe em geração,
    // usa isOrInheritsFrom para apanhar o caso herdado.
    private static boolean participates(Class cls, String ownerType, String assocEndClass) {
        if (ownerType != null && ownerType.equals(assocEndClass)) return true;
        return cls != null && cls.getName().equals(ownerType) && cls.isOrInheritsFrom(assocEndClass);
    }

    // procura a associação entre ownerType e otherType, navegável do lado do owner
    private static Association findAssoc(List<Association> assocs, Class cls, String ownerType, String otherType) {
        for (Association a : assocs){
            if (participates(cls, ownerType, a.getSourceClass()) && a.getTargetClass().equals(otherType) && a.isTargetNavigable())
                return a;
            if (participates(cls, ownerType, a.getTargetClass()) && a.getSourceClass().equals(otherType) && a.isSourceNavigable())
                return a;
        }
        return null;
    }

    private static String fieldNameFor(Association a, Class cls, String ownerType) {
        // lado a partir do owner: se owner é (ou herda) o source, o campo refere o target, e vice-versa
        if (participates(cls, ownerType, a.getSourceClass())) {
            return GeneratorUtils.makeAttributeName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
        }
        return GeneratorUtils.makeAttributeName(a.getSourceClass(), a.getSourceRole(), a.getSourceMultiplicity());
    }

    // a mensagem do actor para lifeline: source pode ser null ou um Actor (type == null)
    public static Message findEntryMessage(Interaction interaction) {
        if (interaction == null) return null;
        return firstMessage(interaction.getElements());
    }

    private static Message firstMessage(List<Object> elements) {
        for (Object el : elements) {
            if (el instanceof Message m) return m;
            if (el instanceof Fragment f) {
                for (Operand op : f.getOperands()) {
                    Message inner = firstMessage(op.getElements());
                    if (inner != null) return inner;
                }
            }
        }
        return null;
    }


    // procura associação cujo field name no lado do owner é attrName
    // reutiliza fieldNameFor para a derivação role-ou-default
    private static Association findAssocByField(List<Association> assocs, Class cls, String ownerType, String attrName) {
        for (Association a : assocs) {
            if (!participates(cls, ownerType, a.getSourceClass()) && !participates(cls, ownerType, a.getTargetClass())) continue;
            if (attrName.equals(fieldNameFor(a, cls, ownerType))) return a;
        }
        return null;
    }

}
