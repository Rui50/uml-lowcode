package generator;

import model.Association;
import model.Attribute;
import model.Class;
import model.Operation;
import model.Parameter;
import model.sequence.Fragment;
import model.sequence.Interaction;
import model.sequence.Message;
import model.sequence.Operand;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneratorUtils {

    public static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * operações "efetivas" de uma classe: próprias + herdadas, com o override a
     * "ganhar". getAllOperations devolve as próprias primeiro, por isso a primeira
     * ocorrência de cada nome é a mais derivada (o override).
     */
    public static List<Operation> effectiveOperations(Class cls) {
        Map<String, Operation> byName = new LinkedHashMap<>();
        for (Operation op : cls.getAllOperations()) {
            byName.putIfAbsent(op.getName(), op);
        }
        return new ArrayList<>(byName.values());
    }

    /**
     * Classe que realmente declara a operação (sobe na hierarquia até a encontrar);
     * para uma op herdada por StudentMember, devolve Member.
     *
     * Necessário porque uma operação de uma superclasse abstrata  (Member.redeemPoints Staffmember.redeempoints)
     * é gerada nos serviços/controllers das subclasses concretas (StudentMember, StaffMember),
     * mas o seu diagrama de sequência foi desenhado em termos da classe que a declara —
     * MemberService, memberId, lifeline ': Member'. Devolver este "owner" deixa a geração
     * ler o diagrama nesses termos enquanto emite para a classe concreta. Sem isto, procurava
     * StudentMemberService / studentMemberId e não encontrava a mensagem de entrada, o
     * parâmetro de self, nem a lifeline do service — e rebentava.
     */
    public static Class declaringClassOf(Class cls, Operation op) {
        for (Class c = cls; c != null; c = c.getParentClass()) {
            if (c.getOperations().contains(op)) return c;
        }
        return cls;
    }

    /**
     * devolve o tipo de referência (wrapper) de um primitivo; necessário quando
     * o campo tem de poder ser null (ex.: campo de CreateDTO com valor por defeito).
     * tipos que já são de referência ficam inalterados.
     */
    public static String fixType(String type) {
        if (type == null) return null;
        switch (type) {
            case "int":     return "Integer";
            case "long":    return "Long";
            case "short":   return "Short";
            case "byte":    return "Byte";
            case "boolean": return "Boolean";
            case "char":    return "Character";
            case "double":  return "Double";
            case "float":   return "Float";
            default:        return type;   // já é tipo de referência
        }
    }

    public static String lowercaseFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // List<Teste.Aluno> -> List<Aluno>
    public static String removePackageNames(String type) {
        if (type == null) return null;
        return type.replaceAll("\\b([A-Za-z_$][\\w$]*\\.)+([A-Za-z_$][\\w$]*)\\b", "$2");
    }

    // expressão de return é última reply-message para o ator (lifeline sem tipo)
    public static String findReplyToActor(List<Object> elements) {
        String last = null;
        for (Object el : elements) {
            if (el instanceof Message m
                    && "reply".equals(m.getMessageSort())
                    && m.getTarget() != null
                    && m.getTarget().getType() == null) {
                String expr = m.getRawName() != null ? m.getRawName() : m.getName();
                if (expr != null && !expr.isBlank()) last = expr.trim();
            } else if (el instanceof Fragment f) {
                for (Operand op : f.getOperands()) {
                    String inner = findReplyToActor(op.getElements());
                    if (inner != null) last = inner;
                }
            }
        }
        return last;
    }

    // texto entre o primeiro '(' e o último ')'
    public static String extractParen(String s) {
        if (s == null) return "";
        int open = s.indexOf('(');
        int close = s.lastIndexOf(')');
        if (open == -1 || close == -1 || close <= open) return "";
        return s.substring(open + 1, close).trim();
    }

    public static boolean checkMultiplicity(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }

    public static String makeAttributeName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) return role.trim();
        String base = lowercaseFirst(className);
        return checkMultiplicity(multiplicity) ? base + "s" : base;
    }

    // extrair o tipo "base" de retorno e parametros: List<Aluno> -> Aluno, Map<Integer, Turma> -> Turma
    public static String extractBaseType(String type) {
        if (type == null) return null;
        type = removePackageNames(type.trim());
        if (type.contains("<")) {
            int start = type.indexOf("<") + 1;
            int end = type.lastIndexOf(">");
            if (start < end) {
                String generic = type.substring(start, end).trim();
                if (generic.contains(",")) {
                    String[] parts = generic.split(",");
                    return extractBaseType(parts[parts.length - 1].trim());
                }
                return extractBaseType(generic);
            }
        }
        return type;
    }

    // entidades referenciadas em parametros / return de operações
    public static List<String> getModelsToImportFromOperations(Class cls, List<Class> classes) {
        Set<String> entityNames = new HashSet<>();
        for (Class c : classes) entityNames.add(c.getName());

        Set<String> entitiesToImport = new HashSet<>();
        for (Operation op : effectiveOperations(cls)) {   // inclui herdadas: os seus tipos também são usados nos métodos/stubs gerados
            // a classe que declara a op (owner) é o tipo do 'self' carregado no service
            // de uma op herdada (ex: Member member = ...), logo tem de ser importada
            Class owner = declaringClassOf(cls, op);
            if (!owner.getName().equals(cls.getName())) {
                entitiesToImport.add(owner.getName());
            }
            for (Parameter param : op.getParameters()) {
                String baseType = extractBaseType(param.getType());
                if (baseType != null && entityNames.contains(baseType) && !baseType.equals(cls.getName())) {
                    entitiesToImport.add(baseType);
                }
            }
            String returnType = extractBaseType(op.getReturnType());
            if (returnType != null && entityNames.contains(returnType) && !returnType.equals(cls.getName())) {
                entitiesToImport.add(returnType);
            }
        }
        return new ArrayList<>(entitiesToImport);
    }

    // entidades referenciadas em assocs navegaveis
    public static List<String> getModelsToImportFromAssociations(Class cls, List<Association> associations) {
        Set<String> entitiesToImport = new HashSet<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String relatedClass = assoc.getTargetClass();
                if (!relatedClass.equals(cls.getName())) entitiesToImport.add(relatedClass);
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String relatedClass = assoc.getSourceClass();
                if (!relatedClass.equals(cls.getName())) entitiesToImport.add(relatedClass);
            }
        }
        return new ArrayList<>(entitiesToImport);
    }

    public static Class findClassByName(List<Class> classes, String name) {
        for (Class c : classes) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    /**
     * corrige acesso a Maps (campos com qualifier): .stream() -> .values().stream()
     */
    public static String fixMapAccess(String java, Class cls, List<Association> associations) {
        if (java == null) return null;
        if (cls == null) return java;   // interfaces have no associations/map fields
        List<String> mapFields = getMapFieldNames(cls, associations);
        for (String field : mapFields) {
            java = java.replace("this." + field + ".stream()", "this." + field + ".values().stream()");
        }
        return java;
    }

    /**
     * os atributos derivados não têm campo, apenas getter. O visitor traduz
     * self.<attr> como acesso direto a campo (this.<attr>), o que não compila
     * para um derivado. Reescrevemos esses acessos para a chamada do getter
     * (this.get<Attr>()). Inclui derivados herdados, porque getAllAttributes
     * percorre a cadeia de pais.
     */
    public static String fixDerivedAccess(String java, Class cls) {
        if (java == null || cls == null) return java;
        for (Attribute a : cls.getAllAttributes()) {
            if (!a.hasDerive()) continue;
            String name = a.getName();
            String replacement = Matcher.quoteReplacement(
                    "this.get" + capitalizeFirst(name) + "()");
            // \b para não apanhar prefixos (this.activeLoansX)
            java = java.replaceAll("\\bthis\\." + Pattern.quote(name) + "\\b", replacement);
        }
        return java;
    }

    /**
     * devolve nomes dos campos que são Map (assoc tem qualifier)
     */
    public static List<String> getMapFieldNames(Class cls, List<Association> associations) {
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
     * Constrói a assinatura Java de uma operation
     * para interfaces
     * Exemplo: "void inscrever(Turma t)"
     */
    public static String buildSignature(Operation op) {
        StringBuilder sb = new StringBuilder();
        sb.append(op.getReturnType() == null ? "void" : op.getReturnType());
        sb.append(" ").append(op.getName()).append("(");
        List<Parameter> params = op.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).getType()).append(" ").append(params.get(i).getName());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Repositórios que um menu/serviço de uma classe precisa: os das classes
     * relacionadas navegáveis + os tipos-domínio usados como parâmetros das
     * operações com diagrama de sequência.
     */
    public static Set<String> getNeededRepositories(Class cls, List<Association> associations, List<Class> allClasses) {
        Set<String> related = new LinkedHashSet<>();
        for (Association assoc : associations) {
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()) {
                related.add(assoc.getTargetClass());
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()) {
                related.add(assoc.getSourceClass());
            }
        }
        for (Operation op : cls.getOperations()) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;
            for (Parameter param : op.getParameters()) {
                String type = param.getType();
                // só incluir se for uma classe-domínio (corresponde a uma das classes conhecidas)
                for (Class other : allClasses) {
                    if (other.getName().equals(type) && !type.equals(cls.getName())) {
                        related.add(type);
                    }
                }
            }
        }
        return related;
    }

    // apaga recursivamente um diretório e o seu conteúdo (usado para limpar o output antes de gerar)
    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    // nome da variável de repositório de uma classe (ex: Aluno -> alunoRepository)
    public static String repoVariable(String className) {
        return className.substring(0, 1).toLowerCase() + className.substring(1) + "Repository";
    }

    // saber se a classe é pai de outra (tem subclasses), para gerar @Inheritance / SuperBuilder
    public static boolean isParent(Class cls, List<Class> allClasses) {
        for (Class c : allClasses) {
            if (c.getParentClass() != null && c.getParentClass().getName().equals(cls.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devolve o nome do atributo do OUTRO lado de uma associação bidirecional
     * (usado em @JsonIgnoreProperties e no mappedBy). side = "source"/"target"
     * indica de que lado estamos a olhar.
     */
    public static String getOtherSideAttributeName(Association assoc, String side) {
        if (side.equals("source")) {
            return makeAttributeName(
                    assoc.getTargetClass(),
                    assoc.getTargetRole(),
                    assoc.getTargetMultiplicity()
            );
        } else {
            return makeAttributeName(
                    assoc.getSourceClass(),
                    assoc.getSourceRole(),
                    assoc.getSourceMultiplicity()
            );
        }
    }
}
