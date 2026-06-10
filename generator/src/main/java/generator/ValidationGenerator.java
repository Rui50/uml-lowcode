package generator;

import model.Class;
import model.Interface;
import model.annotations.Invariant;

import java.io.PrintWriter;

public class ValidationGenerator {

    /**
     * gera função validate() para uma classe
     * public java.util.List<String> validate() {
     *     java.util.List<String> errors = new java.util.ArrayList<>();
     *
     *     // invariante teste : MaxAlunos
     *     if (!(this.alunos.size() <= 30)) {
     *         errors.add("Invariant 'MaxAlunos' violated: self.alunos->size() <= 30");
     *     }
     *
     *     return errors;
     * }
     */
    public void generateValidateMethod(PrintWriter writer, Class cls) {
        // gera validate se tiver invariantes proprias, herdadas de interface, ou herdadas de parent
        if (!cls.hasAnyInvariants()) return;
        boolean parentHas = cls.getParentClass() != null && cls.getParentClass().hasAnyInvariants();

        writer.println("    // Validation (para ja so invariantes OCL simples");
        writer.println();

        // javadoc
        writer.println("    /**");
        writer.println("     * Validate entity with OCL");
        writer.println("     * ");
        for (Invariant inv : cls.getInvariants()) {
            writer.println("     * - " + inv.getName() + ": " + inv.getOclExpression());
        }
        for (Interface iface : cls.getRealizations()) {
            for (Invariant inv : iface.getInvariants()) {
                writer.println("     * - [" + iface.getName() + "] " + inv.getName() + ": " +
                        inv.getOclExpression());
            }
        }
        writer.println("     * ");
        writer.println("     * @return list of validation errors (empty if valid)");
        writer.println("     */");

        // assign metodo
        writer.println("    public List<String> validate() {");
        writer.println("       List<String> errors = new ArrayList<>();");
        if (parentHas) {
            writer.println("        errors.addAll(super.validate());");
        }
        writer.println();

        // validation para cada invariant (ANTLR4 OCL -> Java)
        for (Invariant inv : cls.getInvariants()) {
            generateInvariantCheck(writer, inv, null);
        }
        for (Interface iface : cls.getRealizations()) {
            for (Invariant inv : iface.getInvariants()) {
                generateInvariantCheck(writer, inv, iface.getName());
            }
        }

        writer.println("        return errors;");
        writer.println("    }");
        writer.println();
    }

    /**
     *
     */
    private void generateInvariantCheck(PrintWriter writer, Invariant inv, String fromInterface) {
        String javaExpression = inv.getJavaExpression();
        String prefix = (fromInterface != null) ? "[" + fromInterface + "] " : "";
        String errorMessage = (inv.getMessage() != null && !inv.getMessage().isEmpty())
                ? prefix + inv.getMessage()
                : prefix + "Invariant '" + inv.getName() + "' violated.";

        writer.println("        // Invariant: " + (fromInterface != null ? "(from " + fromInterface + ") "
                : "") + inv.getName());
        writer.println("        if (!(" + javaExpression + ")) {");
        writer.println("            errors.add(\"" + errorMessage + "\");");
        writer.println("        }");
        writer.println();
    }

}