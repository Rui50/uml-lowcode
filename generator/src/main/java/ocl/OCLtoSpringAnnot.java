package ocl;

import model.Class;
import model.Attribute;
import model.annotations.Invariant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OCLtoSpringAnnot {

    // que tipo de campo é que uma regra aceita (para a verificação de tipo)
    enum AppliesTo {
        NUMERIC, STRING, BOOLEAN, REFERENCE, COLLECTION
    }

    // uma regra é regex -> anotação (com $1,$2 = grupos capturados)
    public static class Rule {
        Pattern pattern;
        String template;
        AppliesTo kind;

        public Rule(String regex, String template, AppliesTo kind) {
            this.pattern = Pattern.compile(regex);
            this.template = template;
            this.kind = kind;
        }
    }

    // resultado: anotações por campo + invariantes mapeadas (que saem do validate())
    public static class Result {
        public Map<String, List<String>> annotationsByField = new LinkedHashMap<>();
        public Set<Invariant> consumed = new HashSet<>();
    }

    // ordem importa: regras específicas antes das gerais (ex: ">= 0" antes de ">= N")
    static final List<Rule> RULES = List.of(
            new Rule("(\\w+) <> null",                "@NotNull",        AppliesTo.REFERENCE),
            new Rule("(\\w+) <> ''",                  "@NotEmpty",       AppliesTo.STRING),
            new Rule("(\\w+) >= 0",                   "@PositiveOrZero", AppliesTo.NUMERIC),
            new Rule("(\\w+) > 0",                    "@Positive",       AppliesTo.NUMERIC),
            new Rule("(\\w+) <= 0",                   "@NegativeOrZero", AppliesTo.NUMERIC),
            new Rule("(\\w+) < 0",                    "@Negative",       AppliesTo.NUMERIC),
            new Rule("(\\w+) >= (\\d+)",              "@Min($2)",        AppliesTo.NUMERIC),
            new Rule("(\\w+) <= (\\d+)",              "@Max($2)",        AppliesTo.NUMERIC),
            new Rule("(\\w+)\\.size\\(\\) >= (\\d+)", "@Size(min = $2)", AppliesTo.STRING),
            new Rule("(\\w+)\\.size\\(\\) <= (\\d+)", "@Size(max = $2)", AppliesTo.STRING),
            new Rule("(\\w+)->notEmpty\\(\\)",        "@NotEmpty",       AppliesTo.COLLECTION),
            new Rule("(\\w+) = true",                 "@AssertTrue",     AppliesTo.BOOLEAN),
            new Rule("(\\w+) = false",                "@AssertFalse",    AppliesTo.BOOLEAN),
            new Rule("not (\\w+)",                    "@AssertFalse",    AppliesTo.BOOLEAN),
            new Rule("(\\w+)",                        "@AssertTrue",     AppliesTo.BOOLEAN) // bare boolean em ultimo
            // @Pattern: regra especial (precisa escapar a regex) — tratada à parte no map()
    );

    // mapeia as invariantes de uma classe -> anotações por campo + invariantes consumidas
    public static Result map(Class cls) {
        Result result = new Result();
        for (Invariant inv : cls.getInvariants()) {
            List<String[]> mapped = mapInvariant(inv, cls);   // null = não-mapeável
            if (mapped == null) continue;                     // fica no validate()
            for (String[] fa : mapped) {
                result.annotationsByField.computeIfAbsent(fa[0], k -> new ArrayList<>()).add(fa[1]);
            }
            result.consumed.add(inv);
        }
        return result;
    }

    // mapeia UMA invariante: as partes (split por "and") têm todas de mapear (tudo-ou-nada),
    // senão devolve null e a invariante fica no validate()
    private static List<String[]> mapInvariant(Invariant inv, Class cls) {
        List<String[]> mapped = new ArrayList<>();
        for (String part : normalize(inv.getOclExpression()).split(" and ")) {
            String[] fa = annotationFor(part.trim(), cls);
            if (fa == null) return null;     // uma parte falha -> invariante inteira falha
            mapped.add(fa);
        }
        return mapped;
    }

    // limpa a expressão para as regexes casarem
    private static String normalize(String ocl) {
        return ocl.trim()
                .replace("self.", "")        // "self.points >= 0" -> "points >= 0"
                .replaceAll("\\s+", " ");     // colapsar espaços múltiplos
    }

    private static final Set<String> PRIMITIVES =
            Set.of("int", "long", "short", "byte", "double", "float", "boolean", "char");

    private static final Set<String> NUMERICS = Set.of(
            "int", "long", "short", "byte", "double", "float",
            "Integer", "Long", "Short", "Byte", "Double", "Float", "BigInteger", "BigDecimal");

    // o tipo do campo é compatível com o AppliesTo da regra?
    private static boolean fits(String type, AppliesTo kind) {
        switch (kind) {
            case NUMERIC:    return NUMERICS.contains(type);
            case STRING:     return "String".equals(type);
            case BOOLEAN:    return "boolean".equals(type) || "Boolean".equals(type);
            case REFERENCE:  return !PRIMITIVES.contains(type);                  // qualquer não-primitivo
            case COLLECTION: return type.startsWith("List") || type.startsWith("Set")
                                  || type.startsWith("Map") || type.startsWith("Collection");
            default:         return false;
        }
    }

    // só atributos PRÓPRIOS: uma anotação de Bean Validation vive no campo, logo aplica-se a
    // TODAS as instâncias da classe que o declara. Uma invariante numa SUBCLASSE sobre um campo
    // HERDADO tem âmbito só da subclasse — não se pode exprimir como anotação no campo do pai
    // (aplicaria a todos os subtipos). Fica no validate() da subclasse (that only runs for it).
    private static String typeOf(Class cls, String field) {
        for (Attribute a : cls.getAttributes()) {
            if (a.getName().equals(field)) return a.getType();
        }
        return null;
    }

    private static final Pattern MATCHES = Pattern.compile("(\\w+)\\.matches\\('([^']*)'\\)");

    // recebe uma parte da invariante ja normalizada ("points >= 0") e devolve
    // {campo, anotação} se for reconhecida, ou null se não for.
    private static String[] annotationFor(String part, Class cls) {
        // caso especial @Pattern: matches('regex')
        Matcher m = MATCHES.matcher(part);
        if (m.matches()) {
            String type = typeOf(cls, m.group(1)); // grupo 1 = camp
            if(type != null && fits(type, AppliesTo.STRING)) { // matches é só de texto
                // escapar para caber numa string Java: \ -> \\  e  " -> \"
                String regex = m.group(2).replace("\\", "\\\\").replace("\"", "\\\"");
                return new String[]{ m.group(1), "@Pattern(regexp = \"" + regex + "\")" };
            }
        }

        // tabela de regras: a 1ª que casa em FORMA e TIPO ganha
        for (Rule rule : RULES) {
            Matcher m2 = rule.pattern.matcher(part);
            if (!m2.matches()) continue;            // a forma não bate -> próxima regra

            String field = m2.group(1);
            String type = typeOf(cls, field);

            // o campo tem de existir E o tipo ser compatível (ex: @Positive só em numérico),
            // senão tenta a regra seguinte
            if (type == null || !fits(type, rule.kind)) continue;

            // a anotação é o template; se a regra capturou um número (grupo 2), mete-o no $2
            // ex: "@Min($2)" com grupo 2 = "0"  ->  "@Min(0)"
            String ann = rule.template;
            if (m2.groupCount() == 2) {
                ann = ann.replace("$2", m2.group(2));
            }
            return new String[]{ field, ann };
        }

        return null; // não reconhecida
    }

}
