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
     * operações "efetivas" de uma classe
     * proprias + herdadas
     */
    public static List<Operation> effectiveOperations(Class cls) {
        // dedup and ordem
        Map<String, Operation> byName = new LinkedHashMap<>();
        for (Operation op : cls.getAllOperations()) {
            byName.putIfAbsent(op.getName(), op);
        }
        return new ArrayList<>(byName.values());
    }

    /**
     * Classe que declara a operação
     * ex é uma op herdada por StudentMember -> devolve Member
     *
     */
    public static Class declaringClassOf(Class cls, Operation op) {
        for (Class c = cls; c != null; c = c.getParentClass()) {
            if (c.getOperations().contains(op)) return c;
        }
        return cls;
    }

    public static List<Attribute> ancestorAttributes(Class cls) {
        List<Attribute> attrs = new ArrayList<>();
        for (Class p = cls.getParentClass(); p != null; p = p.getParentClass()) {
            attrs.addAll(0, p.getAttributes());   // insere à frente, para o mais antigo ficar primeir
        }
        return attrs;
    }

    /**
     * troca int/long/boolean/... pela versão que aceita null (Integer/Long/Boolean)
     * usado quando o campo tem de poder ser null (como nos campos de CreateDTO não preenchido)
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

    public static boolean isPrimitive(String type) {
        return type != null && !fixType(type).equals(type);
    }

    public static String lowercaseFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // gera um nome para a db valido a partir do nome da app. "My Library App" -> "my_library_app"
    public static String dbName(String appName) {
        String slug = appName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")  // espacos/simbolos -> '_'
                .replaceAll("^_+|_+$", ""); // tira '_' do início/fim
        if (slug.isEmpty()) slug = "app"; // nome só com símbolos -> evita nome vazio
        if (Character.isDigit(slug.charAt(0))) slug = "db_" + slug;  // identificador SQL não pode começar por dígito
        return slug;
    }

    // List<Teste.Aluno> -> List<Aluno>
    public static String removePackageNames(String type) {
        if (type == null) return null;
        return type.replaceAll("\\b([A-Za-z_$][\\w$]*\\.)+([A-Za-z_$][\\w$]*)\\b", "$2");
    }

    // expressão de return é ultima reply-message para o ator (lifeline sem tipo)
    public static String findReplyToActor(List<Object> elements) {
        String last = null;
        for (Object el : elements) {
            // reply cujo target é lifeline sem tipo = autor
            if (el instanceof Message m && "reply".equals(m.getMessageSort()) && m.getTarget() != null
                    && m.getTarget().getType() == null) {
                String expr;
                if (m.getRawName() != null) {
                    expr = m.getRawName();
                } else {
                    expr = m.getName();
                }
                if (expr != null && !expr.isBlank()) last = expr.trim();
            } else if (el instanceof Fragment f) {
                // pode tar dentro dos operandos
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

    // gera o @Column: sempre com o name (nome do atributo) unique/precision/scale só se definidos
    public static String columnAnnotation(Attribute atr) {
        // nome da coluna
        String colName = (atr.getColumnName() != null) ? atr.getColumnName() : atr.getName();

        // anotacao @Column
        StringBuilder sb = new StringBuilder("@Column(name = \"").append(colName).append("\"");
        if (atr.isUnique()) sb.append(", unique = true");
        if (atr.getPrecision() != null) sb.append(", precision = ").append(atr.getPrecision());
        // scale só se aplica a BigDecimal/BigInteger; em float/double o Hibernate rebenta no arranque
        if (atr.getScale() != null && isFixedPoint(atr.getType())) {
            sb.append(", scale = ").append(atr.getScale());
        }
        return sb.append(")").toString();
    }

    private static boolean isFixedPoint(String type) {
        return "BigDecimal".equals(type) || "BigInteger".equals(type);
    }

    /**
     * no JPA bidirecional só um lado pode ser dono (o que tem a FK/join table) e o outro leva mappedBy
     * esta func diz se este lado é o dono, oque decide é a navegabilidade e a forma da relação.
     *
     * unidirecional      -> o lado navegável é sempre o dono
     * bidirecional M:N e 1:1 -> o target é o dono
     * bidirecional 1:N (coleção) -> não é dono (leva mappedBy)
     * bidirecional N:1 (singular) -> é dono (tem a FK)
     */
    // numa auto associação os dois lados tem a mesma classe, por isso o lado
    // tem de vir como argumento porque a classe sozinha não os distingue
    public static boolean isOwningSide(Association assoc, boolean isSource) {

        // verificar se este lado é navegável
        boolean navigableFromHere = isSource ? assoc.isTargetNavigable() : assoc.isSourceNavigable();
        if (!navigableFromHere) return false;   // nem sequer tem campo

        // se o outro lado nao for navegável e este é, é unidirecional -> este lado é dono
        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();
        if (!isBidirectional) return true;      // unidirecional o lado navegável é o dono

        // bidirecional decide-se pela forma (multiplicidade) do campo deste lado vs o do lado inverso
        String thisAttributeMul    = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
        String inverseAttributeMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();

        // um lado é "coleção" se a multiplicidade for * OU se for a ponta do Map (qualifier)
        boolean attributeIsCollection   = checkMultiplicity(thisAttributeMul)    || assoc.isMapEnd(isSource);
        boolean inverseIsCollection = checkMultiplicity(inverseAttributeMul) || assoc.isMapEnd(!isSource);

        // em M:N qualifier o dono é a ponta do QUALIFIER
        if (attributeIsCollection && inverseIsCollection && assoc.hasQualifier()) {
            return assoc.isMapEnd(isSource);
        }
        if ( attributeIsCollection &&  inverseIsCollection) return !isSource;   // M:N -> target é dono
        if ( attributeIsCollection && !inverseIsCollection) return false;   // 1:N -> este é o lado "um" fica mappedBy
        if (!attributeIsCollection &&  inverseIsCollection) return true;    // N:1 -> este é o lado "muitos" tem a FK
        return !isSource;   // 1:1 -> target é dono
    }

    public static boolean isNavigableTo(String className, Association a) {
        return a.getSourceClass().equals(className) ? a.isTargetNavigable() : a.isSourceNavigable();
    }

    // a classe do outro lado
    public static String otherClassOf(String className, Association a) {
        return a.getSourceClass().equals(className) ? a.getTargetClass() : a.getSourceClass();
    }

    // o campo desta classe é coleção? (outro lado "muitos" ou associação de map (qualifier))
    public static boolean isCollectionOf(String className, Association a) {
        boolean s = a.getSourceClass().equals(className);
        return checkMultiplicity(s ? a.getTargetMultiplicity() : a.getSourceMultiplicity()) || a.hasQualifier();
    }

    /**
     * Nome da classe que declar" esta associação na hierarquia:
     *   - o próprio nome, nome do pai, ou null se nao participa (ou só herda de pai que ja tem service)
     *
     * para Spring porque o sync do lado inverso usa o nome do campo, e esse campo é derivado pela classe que declara a assoc
     */
    public static String assocDeclaringClass(Class cls, Association a) {
        if (cls.isOrInheritsFrom(a.getSourceClass()) && a.isTargetNavigable())
            return ownOrAbstract(cls, a.getSourceClass());
        if (cls.isOrInheritsFrom(a.getTargetClass()) && a.isSourceNavigable())
            return ownOrAbstract(cls, a.getTargetClass());
        return null;
    }

    // onde a assoc ta declarada /
    // na propria classe ou pai abstrato (que nao tem service) devolve o nome
    // se nao null
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
        for (Operation op : effectiveOperations(cls)) {
            // a classe que declara a operacao
            Class owner = declaringClassOf(cls, op);
            if (!owner.getName().equals(cls.getName())) {
                entitiesToImport.add(owner.getName());
            }
            for (Parameter param : op.getParameters()) {
                String baseType = extractBaseType(param.getType());
                // se param for uma entidade e nao a propria classe
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
        // entidades a importar por causa das associações incluindo as herdadas de um pai abstrato
        Set<String> entitiesToImport = new HashSet<>();

        for (Association assoc : associations) {
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
     * corrige acesso a Maps (campos com qualifier): o OCL traduz para Collection
     */
    public static String fixMapAccess(String java, Class cls, List<Association> associations) {
        if (java == null) return null;
        if (cls == null) return java;   // interfaces
        List<String> mapAttributes = getMapAttributeNames(cls, associations);
        for (String attribute : mapAttributes) {
            // o acesso à coleção já vem como getter (this.getAlunos())
            String access = "this.get" + capitalizeFirst(attribute) + "()";
            java = java.replace(access + ".stream()", access + ".values().stream()");
            java = java.replace(access + ".contains(", access + ".containsValue(");
            java = java.replace(access + ".containsAll(", access + ".values().containsAll(");
            java = java.replace("frequency(" + access + ",", "frequency(" + access + ".values(),");
            java = java.replace("new HashSet<>(" + access + ")", "new HashSet<>(" + access + ".values())");
            java = java.replace("new ArrayList<>(" + access + ")", "new ArrayList<>(" + access + ".values())");
        }
        return java;
    }

    /**
     * o size() do OCL é o comprimento da String
     * so da prop clase
     */
    public static String fixStringSize(String java, Class cls) {
        if (java == null) return null;
        if (cls == null) return java;   // interfaces
        for (Attribute attribute : cls.getAllAttributes()) {
            if (!"String".equals(attribute.getType())) continue;
            String access = "this.get" + capitalizeFirst(attribute.getName()) + "()";
            java = java.replace(access + ".size()", access + ".length()");
        }
        return java;
    }

    /**
     * devolve nomes dos campos que são Map (assoc tem qualifier)
     */
    public static List<String> getMapAttributeNames(Class cls, List<Association> associations) {
        List<String> mapAttributes = new ArrayList<>();
        for (Association assoc : associations) {
            // o Map fica só na ponta que é owner do qualifier (isMapEnd), não nas duas
            // usa o mesmo nome de campo que o gerador de entidades (makeAttributeName) para o fixMapAccess bater certo
            // a associação qualificada pode estar declarada num pai
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable() && assoc.isMapEnd(true)) {
                mapAttributes.add(makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity()));
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable() && assoc.isMapEnd(false)) {
                mapAttributes.add(makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity()));
            }
        }
        return mapAttributes;
    }

    /**
     * constroi a assinatura Java de uma operation para interfaces
     * "void inscrever(Turma t)"
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
     * repos que o menu/serviço de uma classe precisa que sao os das classes relacionadas
     * navegáveis, e os das entidades usadas como argumento das operações com diagrama.
     */
    public static Set<String> getNeededRepositories(Class cls, List<Association> associations, List<Class> allClasses) {
        Set<String> related = new LinkedHashSet<>();
        for (Association assoc : associations) {
            // exclui a própria classe senao numa auto-associação era injetado duas vezes
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()
                    && !assoc.getTargetClass().equals(cls.getName())) {
                related.add(assoc.getTargetClass());
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()
                    && !assoc.getSourceClass().equals(cls.getName())) {
                related.add(assoc.getSourceClass());
            }
        }
        // effectiveOperations inclui as ops herdadas de pai abstrato (também geradas nos menus)
        // Usa os args do entry message (o que gera o findById), não os params da assinatura
        for (Operation op : effectiveOperations(cls)) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;
            Message entry = OperationGenerator.findEntryMessage(interaction);
            if (entry == null) continue;
            for (MessageArgument arg : entry.getArguments()) {
                String type = arg.getType();
                // so incluir se o tipo for uma classe
                for (Class other : allClasses) {
                    if (other.getName().equals(type) && !type.equals(cls.getName())) {
                        related.add(type);
                    }
                }
            }
        }
        return related;
    }

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
     * fix mensagens de erro do utilizador
     * aspas, \ e \n partiam a compilação.
     */
    public static String fixErrorMessage(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    // nome da variável de repositório de uma classe (Aluno -> alunoRepository)
    public static String repoVariable(String className) {
        return lowercaseFirst(className) + "Repository";
    }

    // nome da tabela JPA que foi overrided do utilizador (painel Persistence), senão o nome da classe em minúsculas
    public static String tableName(Class cls) {
        String t = cls.getTableName();
        return (t != null && !t.isBlank()) ? t : cls.getName().toLowerCase();
    }

    // tipo de id de uma classe
    public static String idType(Class cls) {
        if (cls != null) {
            return cls.getRoot().getIdAttributeType();
        } else {
            return "Long";
        }
    }

    // nome da var do id da classe
    public static String idAttributeName(Class cls) {
        if (cls != null && cls.getRoot().getIdAttributeName() != null) {
            return cls.getRoot().getIdAttributeName();
        } else {
            return "id";
        }
    }
    public static String idGetter(Class cls) {
        return "get" + capitalizeFirst(idAttributeName(cls));
    }

    // saber se a classe é pai de outra (tem subclasses), para gerar @Inheritance
    public static boolean isParent(Class cls, List<Class> allClasses) {
        for (Class c : allClasses) {
            if (c.getParentClass() != null && c.getParentClass().getName().equals(cls.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * devolve o nome do atributo do outro lado de uma associação, para o mappedBy e o @JsonIgnoreProperties.
     */
    public static String getOtherSideAttributeName(Association assoc, boolean isSource) {
        if (isSource) {
            return makeAttributeName(
                    assoc.getSourceClass(),
                    assoc.getSourceRole(),
                    assoc.getSourceMultiplicity()
            );
        } else {
            return makeAttributeName(
                    assoc.getTargetClass(),
                    assoc.getTargetRole(),
                    assoc.getTargetMultiplicity()
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

    // para nao verificar todas as mensagens se tiver operacao importamos
    public static boolean needsJavaTime(Class cls) {
        if (cls.getAllAttributes().stream().anyMatch(a -> isJavaTimeType(a.getType()))) return true;
        for (Operation op : effectiveOperations(cls)) {
            if (isJavaTimeType(op.getReturnType())) return true;
            if (op.getParameters() != null
                    && op.getParameters().stream().anyMatch(p -> isJavaTimeType(p.getType()))) return true;
            if (op.getSequenceDiagram() != null) return true;
        }
        return false;
    }

    // para saber se é preciso "import java.math.*" para BigDecimal/BigInteger
    public static boolean isJavaMathType(String type) {
        return "BigDecimal".equals(type) || "BigInteger".equals(type);
    }

    public static boolean needsJavaMath(Class cls) {
        if (cls.getAllAttributes().stream().anyMatch(a -> isJavaMathType(a.getType()))) return true;
        for (Operation op : effectiveOperations(cls)) {
            if (isJavaMathType(op.getReturnType())) return true;
            if (op.getParameters() != null
                    && op.getParameters().stream().anyMatch(p -> isJavaMathType(p.getType()))) return true;
            if (op.getSequenceDiagram() != null) return true;
        }
        return false;
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
            case "short": case "Short":
            case "String":
            case "BigDecimal": case "BigInteger":
            case "UUID":
                return true;
            default:
                return isJavaTimeType(type);
        }
    }
}
