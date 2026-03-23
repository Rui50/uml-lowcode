package ocl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
/**
 * ocl Para codigo java
 *
 * para ja base
 * - Attribute access: self.alunos->size()
 * - Comparisons: <=, >=, <, >, =, <>
 * - Boolean ops: and, or, not
 * - Collections: ->size(), ->isEmpty(), ->notEmpty()
 *
 * depois : Iterations
 * - ->forAll(), ->exists(), ->select()
 *
 * - ->collect(), ->sum(), ->avg()
 */

public class OCLTranslator {

    public String translateToJava(String oclExpression) {
        if (oclExpression == null || oclExpression.trim().isEmpty()) {
            return "true";
        }

        String java = oclExpression.trim();

        // transformações base

        // para ja, para coisas simples, isto funciona
        java = java.replaceAll("self\\.(\\w+)", "this.$1");
        java = translateArrowNotation(java);
        java = translateBooleanOperators(java);
        java = translateComparisonOperators(java);

        return java;
    }

    /**
     * ->size() -> .size()
     * -> size() -> .size()
     * ->isEmpty() -> .isEmpty()
     * ->notEmpty() -> !.isEmpty() -> dar fix
     */
    private String translateArrowNotation(String ocl) {
        String result = ocl;

        // Consume optional whitespace before the arrow so `alunos -> size()`
        // becomes `alunos.size()` instead of `alunos .size()`.
        result = result.replaceAll("\\s*-\\s*>\\s*size\\s*\\(\\s*\\)", ".size()");

        // ->isEmpty() or -> isEmpty() -> .isEmpty()
        result = result.replaceAll("\\s*-\\s*>\\s*isEmpty\\s*\\(\\s*\\)", ".isEmpty()");

        // ->notEmpty() or -> notEmpty() -> !.isEmpty()
        result = result.replaceAll("\\s*-\\s*>\\s*notEmpty\\s*\\(\\s*\\)", "!.isEmpty()");

        return result;
    }

    /**
     * ocl boolean to Java
     * <p>
     * and -> &&
     * or -> ||
     * not -> !
     */
    private String translateBooleanOperators(String ocl) {
        String result = ocl;

        // 'and' -> '&&' (word boundaries to avoid matching 'sand', 'band', etc)
        result = result.replaceAll("\\band\\b", "&&");

        // 'or' -> '||'
        result = result.replaceAll("\\bor\\b", "||");

        // 'not' -> '!' (but only as standalone, not in notEmpty which is already handled)
        // For now, simple replacement (Phase 2 will handle more complex cases)
        result = result.replaceAll("\\bnot\\b", "!");

        return result;
    }

    /**
     * Translate OCL comparison operators to Java
     * <p>
     * <> -> !=
     * = -> == (context-aware: only in comparisons, not assignments)
     */
    private String translateComparisonOperators(String ocl) {
        String result = ocl;

        // <> -> !=
        result = result.replace("<>", "!=");

        // = -> == (not if already ==, <=, or >=)
        result = result.replaceAll("\\s=\\s", " == ");

        return result;
    }

    /**
     * @param invariantName name of the invariant
     * @param oclExpression original OCL expression
     * @param customMessage optional custom error message
     * @return Java code block as String
     */
    public String generateValidationCode(String invariantName, String oclExpression, String customMessage) {
        String javaExpression = translateToJava(oclExpression);

        String errorMessage = (customMessage != null && !customMessage.isEmpty())
                ? customMessage
                : "Invariant '" + invariantName + "' violated.";

        // code block
        StringBuilder code = new StringBuilder();
        code.append("        // Invariant: ").append(invariantName).append("\n");
        code.append("        if (!(").append(javaExpression).append(")) {\n");
        code.append("            errors.add(\"").append(errorMessage).append("\");\n");
        code.append("        }");

        return code.toString();
    }
}
