package generator;

import model.Class;
import model.Interface;
import model.annotations.Invariant;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

public class ValidationGenerator {

    /**
     * gera metodos de validação para uma classe
     *
     * validate() -> devolve uma list <String> com os errors, se ta vazia ta valido,
     * inclui herdades de interface e se tem pai super.validate()
     *
     * checkInvariantes() -> corre validate e lança se tiver errors (so para o spring)
     *
     * public List<String> validate() {
     *       List<String> errors = new ArrayList<>();
     *
     *       // inv MaxAlunos
     *       if (!(this.getAlunos().size() <= 30)) {
     *           errors.add("Invariant 'MaxAlunos' violated: self.alunos->size() <= 30");
     *       }
     *
     *       return errors;
     *   }
     */

    // usada pelo cli, a outra leva um set the invs para dar skip (invariantes já transformadas em anotações de Validation Spring)
    public void generateValidateMethod(PrintWriter writer, Class cls) {
        generateValidateMethod(writer, cls, Collections.emptySet());
    }

    // 'skip' = invariantes já transformadas em anotações de Validation (Spring)
    // por isso saem do validate(),vazio nos restantes alvos
    public void generateValidateMethod(PrintWriter writer, Class cls, Set<Invariant> skip) {
        // gera validate se tiver invariantes proprias, herdadas de interface, ou herdadas de parent
        // se todas inv sao skipped cria a função na msm
        if (!cls.hasAnyInvariants()) return;
        boolean parentHas = cls.getParentClass() != null && cls.getParentClass().hasAnyInvariants();

        // javadoc
        writer.println("    /**");
        writer.println("     * Validate entity with OCL");
        writer.println("     * ");

        // invariantes da própria classe
        for (Invariant inv : cls.getInvariants()) {
            if (skip.contains(inv)) continue;
            writer.println("     * - " + inv.getName() + ": " + inv.getOclExpression());
        }

        // invariantes herdadas de interfaces
        for (Interface iface : cls.getRealizations()) {
            for (Invariant inv : iface.getInvariants()) {
                writer.println("     * - [" + iface.getName() + "] " + inv.getName() + ": " +
                        inv.getOclExpression());
            }
        }
        writer.println("     * ");
        writer.println("     * @return list of errors (empty if valid)");
        writer.println("     */");

        // assign metod
        writer.println("    public List<String> validate() {");
        writer.println("        List<String> errors = new ArrayList<>();");
        if (parentHas) {
            writer.println("        errors.addAll(super.validate());");
        }
        writer.println();

        // validation para cada invariant

        for (Invariant inv : cls.getInvariants()) {
            if (skip.contains(inv)) continue;   // ja é anotação spring
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

    // wrapper que lannça o erro em vez de devolver a lista
    public void generateCheckInvariantsMethod(PrintWriter writer, Class cls) {
        if (!cls.hasAnyInvariants()) return;
        boolean parentHas = cls.getParentClass() != null && cls.getParentClass().hasAnyInvariants();
        if (parentHas) return;   // herda o checkInvariants() do pai
        writer.println("    public void checkInvariants() {");
        writer.println("        List<String> errors = validate();");
        writer.println("        if (!errors.isEmpty()) {");
        writer.println("            throw new IllegalArgumentException(\"Validation failed: \" + String.join(\", \", errors));");
        writer.println("        }");
        writer.println("    }");
        writer.println();
    }

    private void generateInvariantCheck(PrintWriter writer, Invariant inv, String fromInterface) {
        // expressão OCL já traduzida para Java
        String javaExpression = inv.getJavaExpression();

        // mensagem de erro a do user se existir, senão default
        String prefix = (fromInterface != null) ? "[" + fromInterface + "] " : "";
        String errorMessage = (inv.getMessage() != null && !inv.getMessage().isEmpty())
                ? prefix + inv.getMessage()
                : prefix + "Invariant '" + inv.getName() + "' violated.";

        //  if (!(expr)) { errors.add("msg") }
        writer.println("        // Invariant: " + (fromInterface != null ? "(from " + fromInterface + ") "
                : "") + inv.getName());
        writer.println("        if (!(" + javaExpression + ")) {");
        writer.println("            errors.add(\"" + GeneratorUtils.fixErrorMessage(errorMessage) + "\");");
        writer.println("        }");
        writer.println();
    }

}