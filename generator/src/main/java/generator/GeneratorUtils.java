package generator;

import model.Association;
import model.Attribute;
import model.Class;
import model.Operation;
import model.Parameter;
import model.sequence.Fragment;
import model.sequence.Interaction;
import model.sequence.Message;
import model.sequence.MessageArgument;
import model.sequence.Operand;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // atributos de todos os ancestrai por ordem raiz-primeiro, para herança a N
    // níveis. addAll(0,..) = prepend, p/ o avô ficar à frente do pai (ordem do super(...))
    public static List<Attribute> ancestorAttributes(Class cls) {
        List<Attribute> attrs = new ArrayList<>();
        for (Class p = cls.getParentClass(); p != null; p = p.getParentClass())
            attrs.addAll(0, p.getAttributes());   // prepend -> raiz-primeiro
        return attrs;
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

    // @Column(...) a partir da config da persistence tab
    // (default = nome do atributo), unique/precision/scale só quando definidos
    public static String columnAnnotation(Attribute atr) {
        StringBuilder sb = new StringBuilder("@Column(name = \"").append(atr.getName()).append("\"");
        if (atr.isUnique()) sb.append(", unique = true");
        if (atr.getPrecision() != null) sb.append(", precision = ").append(atr.getPrecision());
        if (atr.getScale() != null) sb.append(", scale = ").append(atr.getScale());
        return sb.append(")").toString();
    }

    /**
     * Em JPA bidirecional só UM lado pode ser o dono (é ele que tem a FK/join table); o
     * outro leva mappedBy. Esta função diz se ESTE lado é o dono.
     *
     * A entidade de "className" é o DONO desta relação
     * Quem decide é a NAVEGABILIDADE, não o source/target da associação.
     *
     * Regra: unidirecional -> o lado navegável é sempre dono
     * bidirecional depende da forma:
     *   M:N e O:O -> o target é dono;
     *   O:M (coleção) -> inverso (mappedBy);
     *   M:O (singular) -> dono da FK.
     */
    // isSource = este campo é o lado SOURCE da associação. Passa-se o lado em vez de o inferir
    // pela classe porque numa auto-associação as duas pontas têm a mesma classe.
    public static boolean isOwningSide(Association assoc, boolean isSource) {
        boolean navigableFromHere = isSource ? assoc.isTargetNavigable() : assoc.isSourceNavigable();
        if (!navigableFromHere) return false;   // nem sequer tem campo

        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();
        if (!isBidirectional) return true;      // unidirecional: o lado navegável é o dono

        // bidirecional: decide-se pela forma do campo DESTE lado e do campo no lado INVERSO
        String thisFieldMul    = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
        String inverseFieldMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
        // a ponta que tem o Map conta sempre como coleçã
        boolean fieldIsCollection   = checkMultiplicity(thisFieldMul)    || assoc.isMapEnd(isSource);
        boolean inverseIsCollection = checkMultiplicity(inverseFieldMul) || assoc.isMapEnd(!isSource);

        // M:N qualifier: dona é a ponta do QUALIFIER — é lá que está o Map e é lá que a coleção se
        // gere (add/remove por chave). Senão o @JoinTable caía numa ponta e o Map na outra.
        if (fieldIsCollection && inverseIsCollection && assoc.hasQualifier()) {
            return assoc.isMapEnd(isSource);
        }
        if ( fieldIsCollection &&  inverseIsCollection) return !isSource;  // M:N -> target é dono
        if ( fieldIsCollection && !inverseIsCollection) return false;      // O:M -> mappedBy (inverso)
        if (!fieldIsCollection &&  inverseIsCollection) return true;       // M:O -> dono da FK
        return !isSource;                                                  // O:O -> target é dono
    }

    // Decidem o lado por igualdade de nome (não cobrem associações herdadas — ver menus).
    // esta classe consegue navegar para o outro lado nesta associação?
    public static boolean isNavigableTo(String className, Association a) {
        return a.getSourceClass().equals(className) ? a.isTargetNavigable() : a.isSourceNavigable();
    }

    // a classe do outro lado
    public static String otherClassOf(String className, Association a) {
        return a.getSourceClass().equals(className) ? a.getTargetClass() : a.getSourceClass();
    }

    // o campo desta classe para esta associação (combina o outro lado + makeAttributeName)
    public static String fieldNameOf(String className, Association a) {
        boolean s = a.getSourceClass().equals(className);
        return makeAttributeName(s ? a.getTargetClass() : a.getSourceClass(),
                                 s ? a.getTargetRole()  : a.getSourceRole(),
                                 s ? a.getTargetMultiplicity() : a.getSourceMultiplicity());
    }

    // o campo desta classe é coleção? (outro lado "muitos" ou associação de map (qualifier))
    public static boolean isCollectionOf(String className, Association a) {
        boolean s = a.getSourceClass().equals(className);
        return checkMultiplicity(s ? a.getTargetMultiplicity() : a.getSourceMultiplicity()) || a.hasQualifier();
    }

    /**
     * Nome da classe que DECLARA esta associação (a que a "possui" na hierarquia)
     *   - o próprio nome da class se participa diretamente
     *   - o nome do pai ABSTRATO se herda a associação dele;
     *   - null se não participa OU só a herda de um pai normal.
     *
     * Versão "com nome" do isOrInheritsFrom (diz se classe participa e sob que nome).
     * Só é preciso em Spring por duas razões
     * - o sync do lado inverso é POR NOME
     * do campo, derivado do declarante ("Member"), não da subclasse;
     * - o "gate" do abstrato — um pai normal já tem service próprio (polimórfico), logo null.
     */
    public static String assocDeclaringClass(Class cls, Association a) {
        if (cls.isOrInheritsFrom(a.getSourceClass()) && a.isTargetNavigable())
            return ownOrAbstract(cls, a.getSourceClass());
        if (cls.isOrInheritsFrom(a.getTargetClass()) && a.isSourceNavigable())
            return ownOrAbstract(cls, a.getTargetClass());
        return null;
    }

    // Sobe a cadeia de pais a partir da classe até encontrar a classe "endName"
    // (o lado da associação em que a classe "participa") e devolve o seu nome se for o
    // próprio cls ou uma classe abstrata, caso contrário (pai normal) null.
    private static String ownOrAbstract(Class cls, String endName) {
        for (Class c = cls; c != null; c = c.getParentClass()) {
            if (c.getName().equals(endName))
                return (c == cls || c.isAbstract()) ? endName : null;
        }
        return null;
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
            // inclui associações herdadas de pai abstrato (assocDeclaringClass): em
            // Spring materializam-se na subclasse, que precisa de importar o outro lado
            String declarer = assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;
            String relatedClass = otherClassOf(declarer, assoc);
            if (!relatedClass.equals(cls.getName())) entitiesToImport.add(relatedClass);
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
            // o acesso à coleção já vem como getter (this.getAlunos()); um campo-Map precisa
            // de .values() antes do .stream()
            String access = "this.get" + capitalizeFirst(field) + "()";
            java = java.replace(access + ".stream()", access + ".values().stream()");
        }
        return java;
    }

    /**
     * devolve nomes dos campos que são Map (assoc tem qualifier)
     */
    public static List<String> getMapFieldNames(Class cls, List<Association> associations) {
        List<String> mapFields = new ArrayList<>();
        for (Association assoc : associations) {
            // o Map fica só na ponta DONA do qualifier (isMapEnd), não nas duas
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable() && assoc.isMapEnd(true)) {
                String role = assoc.getTargetRole();
                if (role != null && !role.trim().isEmpty()) {
                    mapFields.add(role.trim());
                } else {
                    String base = lowercaseFirst(assoc.getTargetClass());
                    mapFields.add(base + "s");
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable() && assoc.isMapEnd(false)) {
                String role = assoc.getSourceRole();
                if (role != null && !role.trim().isEmpty()) {
                    mapFields.add(role.trim());
                } else {
                    String base = lowercaseFirst(assoc.getSourceClass());
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
            // exclui a própria classe (o seu repo é declarado à parte) — senão uma
            // auto-associação injeta o mesmo repo duas vezes (ex: employeeRepository)
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()
                    && !assoc.getTargetClass().equals(cls.getName())) {
                related.add(assoc.getTargetClass());
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()
                    && !assoc.getSourceClass().equals(cls.getName())) {
                related.add(assoc.getSourceClass());
            }
        }
        // effectiveOperations (não só as próprias): os menus geram também as ops herdadas de pai
        // abstrato, cujos parâmetros-entidade precisam do repo declarado. Usa os args do ENTRY MESSAGE
        // (o que generateCustomOperationMethods realmente lê p/ emitir findById), não os params do
        // class diagram — senão um arg-entidade do entry que não está na assinatura fica sem repo.
        for (Operation op : effectiveOperations(cls)) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;
            Message entry = OperationGenerator.findEntryMessage(interaction);
            if (entry == null) continue;
            for (MessageArgument arg : entry.getArguments()) {
                String type = arg.getType();
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

    /**
     * fix mensagens de erro que é texto livre do utilizador
     * aspas, \ e \n partiam a compilação.
     * ordem - o backslash primeiro, senão re-escapava os escapes seguintes.
     */
    public static String fixErrorMessage(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    // nome da variável de repositório de uma classe (ex: Aluno -> alunoRepository)
    public static String repoVariable(String className) {
        return lowercaseFirst(className) + "Repository";
    }

    // nome da tabela JPA: override do utilizador (painel Persistence), senão o nome da classe em minúsculas
    public static String tableName(Class cls) {
        String t = cls.getTableName();
        return (t != null && !t.isBlank()) ? t : cls.getName().toLowerCase();
    }

    // id de uma classe — resolvido pela raiz (subclasses partilham o id do pai)
    // cls null (nome desconhecido) da defaults no tipo Long, campo "id".
    public static String idType(Class cls) {
        return (cls != null) ? cls.getRoot().getIdAttributeType() : "Long";
    }
    public static String idFieldName(Class cls) {
        return (cls != null && cls.getRoot().getIdAttributeName() != null)
                ? cls.getRoot().getIdAttributeName() : "id";
    }
    public static String idGetter(Class cls) {
        return "get" + capitalizeFirst(idFieldName(cls));
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

    // para saber se é preciso "import java.time.*" nas classes/DTOs com atributos de data
    public static boolean isJavaTimeType(String type) {
        if (type == null) return false;
        switch (type) {
            case "LocalDate": case "LocalDateTime": case "LocalTime":
            case "Instant": case "OffsetDateTime": case "ZonedDateTime":
            case "OffsetTime": case "Duration": case "Period":
            case "Year": case "YearMonth": case "MonthDay":
                return true;
            default:
                return false;
        }
    }

    // o que não for simples nem entidade é DTO (@RequestBody)
    public static boolean isSimpleParamType(String type) {
        if (type == null) return false;
        switch (type) {
            case "int": case "Integer":
            case "long": case "Long":
            case "byte": case "Byte":
            case "double": case "Double":
            case "float": case "Float":
            case "boolean": case "Boolean":
            case "char": case "Character":
            case "String":
                return true;
            default:
                return isJavaTimeType(type);
        }
    }
}
