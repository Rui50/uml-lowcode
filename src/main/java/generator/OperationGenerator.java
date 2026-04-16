package generator;

import model.Association;
import model.Class;
import model.Operation;
import model.sequence.*;

import java.io.PrintWriter;
import java.util.List;

public class OperationGenerator {
    // gera a operação PIM
    public static void generateOperation(PrintWriter writer, Class cls, Interaction interaction,
                                         List<Association> associations) {
        Message entry = findEntryMessage(interaction, cls);
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

        generateElements(writer, interaction.getElements(), entry, cls, associations, 2);

        writer.println("    }");
    }
    private static void generateElements(PrintWriter writer, List<Object> elements,
                                         Message entry, Class cls, List<Association> associations, int indent) {
        String indentStr = "    ".repeat(indent);
        for (Object el : elements) {
            if (el instanceof Message) {
                Message msg = (Message) el;
                if (msg.getSource() == null || msg.getSource().getType() == null) continue;
                generateMessage(writer, msg, entry, indentStr);
            } else if (el instanceof Fragment) {
                generateFragment(writer, (Fragment) el, entry, cls, associations, indent);
            }
        }
    }

    private static void generateMessage(PrintWriter writer, Message msg, Message entry, String indent) {
        String receiver;
        if (msg.getTarget().getId().equals(entry.getTarget().getId())) {
            receiver = "this";
        } else {
            receiver = msg.getTarget().getName();
            if (receiver == null || receiver.isEmpty()) {
                receiver = "/* lifeline has no name: " + msg.getTarget().getType() + " */";
            }
        }

        StringBuilder call = new StringBuilder();
        call.append(receiver).append(".").append(msg.getName()).append("(");
        List<MessageArgument> args = msg.getArguments();
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

    private static String makeFieldName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) return role.trim();
        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        boolean multiple = multiplicity != null && (multiplicity.contains("*") || (multiplicity.contains("..") && !multiplicity.equals("0..1")));
        return multiple ? base + "s" : base;
    }

    public static Interaction findInteractionForOperation(Class cls, Operation op, List<Interaction> interactions) {
        if (interactions == null) return null;
        for (Interaction interaction : interactions) {
            Message entry = findEntryMessage(interaction, cls);
            if (entry != null && entry.getName().equals(op.getName())) {
                return interaction;
            }
        }
        return null;
    }

    // a mensagem do actor para lifeline: source pode ser null ou um Actor (type == null)
    private static Message findEntryMessage(Interaction interaction, Class cls) {
        for (Message msg : interaction.getMessages()) {
            if (msg.getTarget() == null) continue;
            if (!cls.getName().equals(msg.getTarget().getType())) continue;
            if (msg.getSource() == null || msg.getSource().getType() == null) {
                return msg;
            }
        }
        return null;
    }
}
