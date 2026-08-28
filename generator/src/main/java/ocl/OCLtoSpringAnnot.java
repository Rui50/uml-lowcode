package ocl;

import generator.RelationshipInfo;
import model.Association;
import model.Class;
import model.Attribute;
import model.annotations.Invariant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OCLtoSpringAnnot {

    // uma regra é regex -> anotação
    public static class Rule {
        Pattern pattern;
        String template;
        Set<String> types;   // tipos permitidos; null = qualquer referência (não pode ser primitivo)

        public Rule(String regex, String template, Set<String> types) {
            this.pattern = Pattern.compile(regex);
            this.template = template;
            this.types = types;
        }
    }

    // resultado é as anotações por campo + invariantes mapeadas (que saem do validate())
    public static class Result {
        public Map<String, List<String>> annotationsByField = new LinkedHashMap<>();
        public Set<Invariant> consumed = new HashSet<>();
    }

    // tipos Java de cada categoria, para a verificação de tipo das regras
    private static final Set<String> NUMBERS    = Set.of(
            "int", "long", "short", "byte", "double", "float",
            "Integer", "Long", "Short", "Byte", "Double", "Float", "BigInteger", "BigDecimal");
    private static final Set<String> STRINGS    = Set.of("String");
    private static final Set<String> BOOLEANS   = Set.of("boolean", "Boolean");
    private static final Set<String> PRIMITIVES = Set.of("int", "long", "short", "byte", "double", "float", "boolean", "char");

    // percorre por ordem e fica na 1 que der match, as específicas tao antes das gerais (">= 0" antes de ">= N")
    static final List<Rule> RULES = List.of(
            new Rule("(\\w+) <> null",                "@NotNull",        null),
            new Rule("(\\w+) <> ''",                  "@NotEmpty",       STRINGS),
            new Rule("(\\w+) >= 0",                   "@PositiveOrZero", NUMBERS),
            new Rule("(\\w+) > 0",                    "@Positive",       NUMBERS),
            new Rule("(\\w+) <= 0",                   "@NegativeOrZero", NUMBERS),
            new Rule("(\\w+) < 0",                    "@Negative",       NUMBERS),
            new Rule("(\\w+) >= (\\d+)",              "@Min($2)",        NUMBERS),
            new Rule("(\\w+) <= (\\d+)",              "@Max($2)",        NUMBERS),
            new Rule("(\\w+)\\.size\\(\\) >= (\\d+)", "@Size(min = $2)", STRINGS),
            new Rule("(\\w+)\\.size\\(\\) <= (\\d+)", "@Size(max = $2)", STRINGS),
            new Rule("(\\w+)->notEmpty\\(\\)",        "@NotEmpty",       null),
            new Rule("(\\w+) = true",                 "@AssertTrue",     BOOLEANS),
            new Rule("(\\w+) = false",                "@AssertFalse",    BOOLEANS),
            new Rule("not (\\w+)",                    "@AssertFalse",    BOOLEANS),
            new Rule("(\\w+)",                        "@AssertTrue",     BOOLEANS)
            // @Pattern (matches(.)) no annotationFor captura uma exp regex  não um número como as outras
    );

    // mapeia as invariantes de uma classe -> anotaçoes por campo + invariantes que saiem do validate.
    public static Result map(Class cls, List<Association> associations) {
        Result result = new Result();
        for (Invariant inv : cls.getInvariants()) {
            List<String[]> mapped = mapInvariant(inv, cls, associations);   // null = nao dá para mapear
            if (mapped == null) continue;   // fica no validate()
            for (String[] fa : mapped) {
                result.annotationsByField.computeIfAbsent(fa[0], k -> new ArrayList<>()).add(fa[1]);
            }
            result.consumed.add(inv);
        }
        return result;
    }

    // mapeia uma invariante, por partes, separa a invariante por "and" e so faz o mapeamento se todas as partes forem mapeaveis.
    // senão devolve null e a invariante fica no validate()
    private static List<String[]> mapInvariant(Invariant inv, Class cls, List<Association> associations) {
        List<String[]> mapped = new ArrayList<>();
        for (String part : normalize(inv.getOclExpression()).split(" and ")) {
            String[] fa = annotationFor(part.trim(), cls, associations);
            if (fa == null) return null;     // uma parte falha ent invariante inteira falha
            mapped.add(fa);
        }
        return mapped;
    }

    // tira o "self." , para reconhecer o campo (senao passava o self. para o regex no caso de se escrever)
    private static String normalize(String ocl) {
        return ocl.trim()
                .replace("self.", "")        // "self.points >= 0" -> "points >= 0"
                .replaceAll("\\s+", " ");     // limpar espaços
    }

    // tipo de um campo próprio atributo ou relação, ou null se não existir/for herdado
    // se for herdado fica null de propósito prorque a anotação no campo do pai aplicaria-se a todos os filhos
    private static String getFieldType(Class cls, String field, List<Association> associations) {
        for (Attribute a : cls.getAttributes()) {
            if (a.getName().equals(field)) return a.getType();
        }
        // relações desta classe (thisClass == cls)
        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            if (rel.field().equals(field) && rel.thisClass().equals(cls.getName())) {
                if (rel.assoc.isMapEnd(rel.isSource))
                    return "Map<" + rel.assoc.getQualifierType() + ", " + rel.otherClass + ">";
                if (rel.isCollection())
                    return "List<" + rel.otherClass + ">";
                return rel.otherClass;
            }
        }
        return null;
    }

    private static final Pattern MATCHES = Pattern.compile("(\\w+)\\.matches\\('([^']*)'\\)");

    // recebe uma parte da invariante  ("points >= 0") e devolve
    // {campo, anotação} se for reconhecida, ou null se não for.
    private static String[] annotationFor(String part, Class cls, List<Association> associations) {
        // caso especial @Pattern: matches('regex')
        Matcher m = MATCHES.matcher(part);
        if (m.matches()) {
            String type = getFieldType(cls, m.group(1), associations); // grupo 1 = campo
            if(type != null && STRINGS.contains(type)) { // matches é só de texto
                // fix no regex do utilizador para não partir a string do @Pattern
                String regex = m.group(2)
                        .replace("\\", "\\\\")   // \d+ -> \\d+ (senao dá "illegal escape")
                        .replace("\"", "\\\"");  // "   -> \"   (senão fecha a string a meio)
                return new String[]{ m.group(1), "@Pattern(regexp = \"" + regex + "\")" };
            }
        }

        // tabela de regras a 1 que da match em forma e tipo
        for (Rule rule : RULES) {
            Matcher m2 = rule.pattern.matcher(part);
            if (!m2.matches()) continue;  // a forma não da match -> próxima regra

            String field = m2.group(1);
            // group(1) tem de ser um campo da classe se não for ignora e vai para o validate()
            String type = getFieldType(cls, field, associations);

            // o campo tem de existir e o tipo ser compatível ( @Positive só em numérico),
            // senão tenta a regra seguinte
            if (type == null) continue;   // campo não existe / herdado
            boolean ok;
            if (rule.types == null) {
                ok = !PRIMITIVES.contains(type);   // qualquer referência (rejeita primitivos)
            } else {
                ok = rule.types.contains(type);    // tipo específico da lista
            }
            if (!ok) continue;

            // troca o $2 do template pelo numero ("@Min($2)" + "0" -> "@Min(0)")
            String ann = rule.template;
            if (m2.groupCount() == 2) {
                ann = ann.replace("$2", m2.group(2));
            }
            return new String[]{ field, ann };
        }

        return null; // naofoi reconhecida
    }

}
