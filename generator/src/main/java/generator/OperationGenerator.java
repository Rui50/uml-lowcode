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

/**
 * Pega numa interação (diagrama de sequência) ligada a uma operação de uma classe
 * e gera o corpo do metodo correspondente.
 *
 * processo:
 *   1. generateOperation — cria metodo, emite preconditions, @pre snapshots,
 *      corpo (via generateElements), result alias, postconditions, return
 *   2. generateElements — itera mensagens e fragmentos; só processa
 *      mensagens enviadas pelo dono da operação
 *   3. generateMessage — gera código para uma mensagem (heurística + fallback)
 *   4. generateFragment — gera código para loop / alt / opt (recursivo)
 *
 * Heurísticas:
 *   - target == entry.target -> "this"; senão usa o nome da lifeline
 *   - <attr>.add(x) ou <attr>.remove(x) -> put/remove para Map (com qualifier),
 *     senão add/remove para coleção simples
 *   - addX(x) / removeX(x) (legacy) -> mesma tradução, lookup por par de tipos
 *   - loops sobre Map -> adaptados para iterar .values()
 *   - reply para o actor -> return statement; sem reply -> sem return (void)
 *
 * OCL:
 *   - preconditions e postconditions são emitidas como if (!(...)) throw IllegalStateException
 *   - @pre nas postconditions -> snapshot do estado declarado antes do corpo
 *   - "result" nas postconditions -> alias declarado depois do corpo, antes das checks
 *
 * Convenções (UML que o utilizador desenha):
 *   - Tipos explícitos em todos os argumentos e returns (a: Aluno, não apenas a)
 *   - Actor (lifeline sem tipo) chama a operação
 *   - Nome da mensagem do actor === nome da operação
 *   - Nome da lifeline === nome do parâmetro (para cross-lifeline calls)
 *   - Lifelines do owner devem ser tipadas (turma:Turma)
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
        if (entry == null) return;

        //writer.print("    public void " + entry.getName() + "(");
        String returnType = operation.getReturnType() != null ? operation.getReturnType() : "void";

        writer.println();
        writer.print("    public " + returnType + " " + entry.getName() + "(");
        for (int i = 0; i < entry.getArguments().size(); i++) {
            if (i > 0) writer.print(", ");
            MessageArgument arg = entry.getArguments().get(i);
            String type = arg.getType() != null ? arg.getType() : "Object";
            writer.print(type + " " + arg.getName());
        }
        writer.println(") {");

        //Operation operation = cls.getOperations().stream().filter(op -> op.getName().equals(entry.getName())).findFirst().orElse(null);

        for (Precondition pre : operation.getPreconditions()){
            writer.println("        if (!(" + pre.getJavaExpression() + ")) {");
            writer.println("            throw new IllegalStateException(\"" + pre.getMessage() + "\");");
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
        String returnExpr = findReplyToActor(interaction.getElements());

        // OCL postcondition pode referir "result" — declaramos alias se algum a usar
        boolean needsResult = operation.getPostconditions().stream()
                .anyMatch(p -> p.getJavaExpression() != null
                        && p.getJavaExpression().matches(".*\\bresult\\b.*"));

        // TODO: estes erros deviam ser apanhados na validação do frontend
        if (!"void".equals(returnType) && returnExpr == null) {
            throw new IllegalStateException(
                    "Operation '" + entry.getName() + "' declares return type '" + returnType
                            + "' but its sequence diagram has no reply message to the actor.");
        }
        if (needsResult && returnExpr == null) {
            throw new IllegalStateException(
                    "Operation '" + entry.getName() + "' has a postcondition referencing 'result' "
                            + "but its sequence diagram has no reply message to the actor.");
        }

        if (needsResult && returnExpr != null && !"void".equals(returnType)) {
            writer.println("        " + returnType + " result = " + returnExpr + ";");
        }

        for (Postcondition post : operation.getPostconditions()) {
            writer.println("        if (!(" + post.getJavaExpression() + ")) {");
            writer.println("            throw new IllegalStateException(\"" + post.getMessage() + "\");");
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
        String receiver;
        if (msg.getTarget().getId().equals(entry.getTarget().getId())) {
            receiver = "this";
        } else {
            receiver = msg.getTarget().getName();
            if (receiver == null || receiver.isEmpty()) {
                throw new IllegalStateException(
                        "Lifeline has no name (type=" + msg.getTarget().getType()
                        + ") in interaction; lifeline name must equal the parameter/local name being called");
            }
        }

        // heurística: add(x) / remove(x) com 1 argumento -> traduzir para acesso à coleção
        /*String name = msg.getName();
        List<MessageArgument> args = msg.getArguments();
        if(args != null && args.size() == 1 && (name.startsWith("add") || name.startsWith("remove"))) {
            MessageArgument arg = args.get(0);
            String ownerType = msg.getTarget().getType();
            Association assoc = findAssoc(associations, ownerType, arg.getType());

            if (assoc != null) {
                String field = fieldNameFor(assoc, ownerType);
                String base = receiver + "." + field;
                String code;
                if (assoc.hasQualifier()){
                    // MAP com PUT(key, value) ou REMOVE(key) — key via getter do qualifier
                    String q = assoc.getQualifierName();
                    String getter = "get" + Character.toUpperCase(q.charAt(0)) + q.substring(1) + "()";
                    code = "add".equals(name)
                            ? base + ".put(" + arg.getName() + "." + getter + ", " + arg.getName() + ");"
                            : base + ".remove(" + arg.getName() + "." + getter + ");";
                } else {
                    // coleção simples: ADD/REMOVE
                    code = base + "." + name + "(" + arg.getName() + ");";
                }
                writer.println(indent + code);
                return;
            }
        }*/

        // heurística: <xx>.add(x) / <xx>.remove(x) ou addX/removeX -> acesso à coleção
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

            // field-style: lookup direto por attrName; antes: lookup por par de tipos
            Association assoc;
            if (attrName != null) {
                assoc = findAssocByField(associations, ownerType, attrName);
            } else {
                assoc = findAssoc(associations, ownerType, arg.getType());
                if (assoc != null) attrName = fieldNameFor(assoc, ownerType);
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
            String type = msg.getReturnType() != null ? msg.getReturnType() : "var";
            writer.println(indent + type + " " + msg.getAssignVariable() + " = " + call + ";");
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

        if ("loop".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard();

            if (guard != null && guard.contains(":")){
                String[] parts = guard.split(":", 2);
                String left = parts[0].trim();
                String collection = parts[1].trim().replace("self.", "this.");
                collection = adaptCollectionAccess(collection, cls, associations);

                String varType = "var";
                String varName;
                if (left.contains(" ")){
                    String[] lp = left.split("\\s+");
                    varType = lp[0];
                    varName = lp[1];
                }
                else {
                    varName = left;
                }

                writer.println(indentStr + "for (" + varType + " " + varName + " : " + collection + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
                writer.println(indentStr + "}");
            } else {
                // while para ja fica aqui
                writer.println(indentStr + "while (" + (guard != null ? guard : "true") + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
                writer.println(indentStr + "}");
            }
        } else if ("opt".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard();
            writer.println(indentStr + "if (" + (guard != null ? guard : "true") + ") {");
            generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
            writer.println(indentStr + "}");
        } else if ("alt".equals(op)) {
            List<Operand> ops = frag.getOperands();
            for (int i = 0; i < ops.size(); i++) {
                Operand operand = ops.get(i);
                String guard = operand.getGuard();
                if (i == 0) {
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
    private static String adaptCollectionAccess(String expr, Class cls, List<Association> associations) {
        if (!expr.startsWith("this.")) return expr;
        String fieldName = expr.substring("this.".length()).trim();

        for (Association assoc : associations) {
            boolean sourceSide = assoc.getSourceClass().equals(cls.getName())
                    && assoc.isTargetNavigable()
                    && fieldName.equals(makeFieldName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity()));
            boolean targetSide = assoc.getTargetClass().equals(cls.getName())
                    && assoc.isSourceNavigable()
                    && fieldName.equals(makeFieldName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity()));

            if ((sourceSide || targetSide) && assoc.hasQualifier()) {
                return expr + ".values()";
            }
        }
        return expr;
    }

    // procura a associação entre ownerType e otherType, navegável do lado do owner
    private static Association findAssoc(List<Association> assocs, String ownerType, String otherType) {
        for (Association a : assocs){
            if (a.getSourceClass().equals(ownerType) && a.getTargetClass().equals(otherType) && a.isTargetNavigable())
                return a;
            if (a.getTargetClass().equals(ownerType) && a.getSourceClass().equals(otherType) && a.isSourceNavigable())
                return a;
        }
        return null;
    }

    private static String fieldNameFor(Association a, String ownerType) {
        // lado a partir do owner: se owner é source, o campo refere o target, e vice-versa
        if (a.getSourceClass().equals(ownerType)) {
            return makeFieldName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
        }
        return makeFieldName(a.getSourceClass(), a.getSourceRole(), a.getSourceMultiplicity());
    }


    private static String makeFieldName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) return role.trim();
        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        boolean multiple = multiplicity != null && (multiplicity.contains("*") || (multiplicity.contains("..") && !multiplicity.equals("0..1")));
        return multiple ? base + "s" : base;
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

    // procura a última reply message cujo target é o actor (lifeline sem tipo)
    // devolve a expressão (raw name da mensagem) que vai ser usada como return.
    //
    // last reply ganha — se houver várias replies, a última é a usada.
    // limitação: não suporta returns condicionais dentro de alt/opt; replies dentro
    // de fragmentos são detectadas mas o return é sempre emitido no fim do método,
    // não no ponto onde a reply aparece no diagrama.
    private static String findReplyToActor(List<Object> elements) {
        String last = null;
        for (Object el : elements) {
            if (el instanceof Message m
                    && "reply".equals(m.getMessageSort())
                    && m.getTarget() != null
                    && m.getTarget().getType() == null) {
                String expr = m.getRawName() != null ? m.getRawName() : m.getName();
                if (expr != null && !expr.isBlank()) last = expr.trim();
            } else if (el instanceof Fragment f) {
                for (Operand op : f.getOperands()) {
                    String inner = findReplyToActor(op.getElements());
                    if (inner != null) last = inner;
                }
            }
        }
        return last;
    }

    // procura associação cujo field name no lado do owner é attrName
    // reutiliza fieldNameFor para a derivação role-ou-default
    private static Association findAssocByField(List<Association> assocs, String ownerType, String attrName) {
        for (Association a : assocs) {
            if (!ownerType.equals(a.getSourceClass()) && !ownerType.equals(a.getTargetClass())) continue;
            if (attrName.equals(fieldNameFor(a, ownerType))) return a;
        }
        return null;
    }

}
