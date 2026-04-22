package generator;

import model.Association;
import model.Class;
import model.annotations.Invariant;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ValidationGenerator {

    private List<Association> associations = new ArrayList<>();

    public ValidationGenerator() {}

    public ValidationGenerator(List<Association> associations) {
        this.associations = associations;
    }

    /**
     * corrige acesso a Maps (campos com qualifier): .stream() -> .values().stream()
     */
    private String fixMapAccess(String java, Class cls) {
        List<String> mapFields = getMapFieldNames(cls);
        for (String field : mapFields) {
            java = java.replace("this." + field + ".stream()", "this." + field + ".values().stream()");
        }
        return java;
    }

    /**
     * devolve nomes dos campos que são Map (assoc tem qualifier)
     */
    private List<String> getMapFieldNames(Class cls) {
        List<String> mapFields = new ArrayList<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable() && assoc.hasQualifier()) {
                String role = assoc.getTargetRole();
                if (role != null && !role.trim().isEmpty()) {
                    mapFields.add(role.trim());
                } else {
                    String base = assoc.getTargetClass().substring(0, 1).toLowerCase() + assoc.getTargetClass().substring(1);
                    mapFields.add(base + "s");
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable() && assoc.hasQualifier()) {
                String role = assoc.getSourceRole();
                if (role != null && !role.trim().isEmpty()) {
                    mapFields.add(role.trim());
                } else {
                    String base = assoc.getSourceClass().substring(0, 1).toLowerCase() + assoc.getSourceClass().substring(1);
                    mapFields.add(base + "s");
                }
            }
        }
        return mapFields;
    }

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
            String javaExpression = fixMapAccess(inv.getJavaExpression(), cls);
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

    /**
     * validate() com uma lista especifica de invs
     * para depois na ferramenta poder selecionar quais
     */
    public void generateValidateMethod(PrintWriter writer, Class cls, List<Invariant> invariants) {
        if (invariants.isEmpty()) {
            return;
        }

        writer.println("    // Validation (para ja so invariantes OCL simples");

        writer.println("    /**");
        writer.println("     * Validate entity OCL");
        writer.println("     * @return list of validation errors (empty if valid)");
        writer.println("     */");
        writer.println("    public List<String> validate() {");
        writer.println("       List<String> errors = new java.util.ArrayList<>();");
        writer.println();

        for (Invariant inv : invariants) {
            // fixMapAcess para casos de Maps
            String javaExpression = fixMapAccess(inv.getJavaExpression(), cls);
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