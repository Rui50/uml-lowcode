package generator;

import model.Association;
import model.Class;
import model.Operation;
import model.annotations.Precondition;
import model.sequence.*;

import java.io.PrintWriter;
import java.util.List;

/**
 * Esta classe pega numa interação (diag sequencia) que está ligado a uma class e gera o corpo da função
 *
 * 1 - generateOperation -> pega na mensagem de entrada (ator para lifeline) e usa os argumentos
 * 2 - generateElements -> itera as mensagens e fragmentos da interação e delega a função respetiva
 * 3 - generateMessage -> gera o codigo correspondente a uma mensagem
 * 4 - generateFragment -> gera o código correspondente a um fragmento (loop, alt, opt) (usa recursão)
 *
 * Heuristicas para facilitar
 * - Se o target de uma mensagem é o mesmo da mensagem de entrada, traduzir para "this"
 *   Senão usa o nome da lifeline (se tiver)
 *
 * - add / remove: mensagens com nome começado por add/remove e 1 argumento, sao consideras para uma coleção
 *                 Usado o tipo do receiver com o tipo do argumento para encontrar a associação correspondente
 *
 * - loops em maps sao automaticamente adaptados para iterar .values()
 *
 * Regras
 *
 * - tudo deve ter o tipo explicado, argumentos, returns etc
 * - para ja é o actor que chama a operação a lifeline em que a operação vive
 * - mensagem da chamada do actor tem de dar match a uma operação da classe
 * - nomes tem de dar match, entre lifelines e mensagens etc
 *
 */

public class OperationGenerator {

    // gera a operação PIM, assinatura, precondições e corpo
    public static void generateOperation(PrintWriter writer, Class cls, Operation operation,
                                         Interaction interaction, List<Association> associations) {
        Message entry = findEntryMessage(interaction);
        if (entry == null) return;

        writer.println();
        writer.print("    public void " + entry.getName() + "(");
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

        generateElements(writer, interaction.getElements(), entry, cls, associations, 2);

        writer.println("    }");
    }

    // Itera as messages e fragments, delegando para generateMessage / generateFragment
    private static void generateElements(PrintWriter writer, List<Object> elements,
                                         Message entry, Class cls, List<Association> associations, int indent) {
        String indentStr = "    ".repeat(indent);
        for (Object el : elements) {
            if (el instanceof Message) {
                Message msg = (Message) el;
                if (msg.getSource() == null || msg.getSource().getType() == null) continue;
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
                receiver = "/* lifeline has no name: " + msg.getTarget().getType() + " */";
            }
        }

        // heurística: add(x) / remove(x) com 1 argumento -> traduzir para acesso à coleção
        String name = msg.getName();
        List<MessageArgument> args = msg.getArguments();
        if(args != null && args.size() == 1 && (name.startsWith("add") || name.startsWith("remove"))) {
            MessageArgument arg = args.get(0);
            String ownerType = msg.getTarget().getType();
            Association assoc = findAssoc(associations, ownerType, arg.getType());

            if (assoc != null) {
                String field = fieldNameFor(assoc, ownerType, arg.getType());
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

    // se a coleção é uma associação qualificada (Map), adiciona .values()
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

    private static String fieldNameFor(Association a, String ownerType, String otherType) {
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

    public static Interaction findInteractionForOperation(Class cls, Operation op, List<Interaction> interactions) {
        /*if (interactions == null) return null;
        for (Interaction interaction : interactions) {
            Message entry = findEntryMessage(interaction, cls);
            if (entry != null && entry.getName().equals(op.getName())) {
                return interaction;
            }
        }
        return null;*/
        return op.getSequenceDiagram();
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
}
