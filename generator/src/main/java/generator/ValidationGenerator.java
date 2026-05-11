package generator;

import model.Class;
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
        if (cls.getInvariants().isEmpty()) {
            return;
        }

        writer.println("    // Validation (para ja so invariantes OCL simples");
        writer.println();

        // Javadoc
        writer.println("    /**");
        writer.println("     * Validate entity with OCL");
        writer.println("     * ");
        for (Invariant inv : cls.getInvariants()) {
            writer.println("     * - " + inv.getName() + ": " + inv.getOclExpression());
        }
        writer.println("     * ");
        writer.println("     * @return list of validation errors (empty if valid)");
        writer.println("     */");

        // Method signature
        writer.println("    public List<String> validate() {");
        writer.println("       List<String> errors = new ArrayList<>();");
        writer.println();

        // Generate validation for each invariant (ANTLR4 OCL -> Java)
        for (Invariant inv : cls.getInvariants()) {
            String javaExpression = inv.getJavaExpression();
            String errorMessage = (inv.getMessage() != null && !inv.getMessage().isEmpty())
                    ? inv.getMessage()
                    : "Invariant '" + inv.getName() + "' violated.";

            writer.println("        // Invariant: " + inv.getName());
            writer.println("        if (!(" + javaExpression + ")) {");
            writer.println("            errors.add(\"" + errorMessage + "\");");
            writer.println("        }");
            writer.println();
        }

        writer.println("        return errors;");
        writer.println("    }");
        writer.println();
    }

}