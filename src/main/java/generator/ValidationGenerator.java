package generator;

import model.Class;
import model.annotations.Invariant;
import ocl.OCLTranslator;

import java.io.PrintWriter;
import java.util.List;

public class ValidationGenerator {
    private OCLTranslator oclTranslator;

    public ValidationGenerator() {
        this.oclTranslator = new OCLTranslator();
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

        // Generate validation for each invariant
        for (Invariant inv : cls.getInvariants()) {
            String validationCode = oclTranslator.generateValidationCode(
                    inv.getName(),
                    inv.getOclExpression(),
                    inv.getMessage()
            );
            writer.println(validationCode);
            writer.println();
        }

        writer.println("        return errors;");
        writer.println("    }");
        writer.println();
    }

    /**
     * validate() method with specific list of invariants
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
            String validationCode = oclTranslator.generateValidationCode(
                    inv.getName(),
                    inv.getOclExpression(),
                    inv.getMessage()
            );
            writer.println(validationCode);
            writer.println();
        }

        writer.println("        return errors;");
        writer.println("    }");
        writer.println();
    }
}