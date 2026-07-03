package generator.spring;

import generator.GeneratorUtils;
import generator.RelationshipInfo;
import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * gerar os dtos, por classe, dois:
 * - CreateDTO: para POST. atributos (sem id gerado, sem os com init — o init obriga o valor
 *   na criação) + ids de relação (single e coleção), toEntity()
 * - UpdateDTO: para PATCH. atributos (sem id) + só ids de relação SINGULARES, sem toEntity()
 * - ResponseDTO: para GET. id + atributos + relacionamento IDs
 *
 * id: CreateDTO exclui sempre o atributo-id (a BD atribui-o);
 * ResponseDTO expõe 'id' por defeito só quando não há id custom; relações usam o tipo/getter do id relacionado. Ver model.Class.
 */
public class SpringDTOGenerator {
    private String baseOutputPath;
    private String basePackage;
    private Set<String> enumNames = new HashSet<>();

    // createdAt/updatedAt expostos no ResponseDTO
    private final boolean timestamps;

    // nome -> Class, para resolver o tipo de id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    public SpringDTOGenerator(String baseOutputPath, String basePackage, boolean timestamps) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
        this.timestamps = timestamps;
    }

    // tipo / getter do id de uma classe (pelo nome), resolvidos a partir da raiz
    private String idTypeOf(String className) {
        return GeneratorUtils.idType(classMap.get(className));
    }
    private String idGetterOf(String className) {
        return GeneratorUtils.idGetter(classMap.get(className));
    }

    public void generateDTOs(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating Spring DTOs...");

        String dtoDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/dto";
        new File(dtoDir).mkdirs();

        enumNames.clear();
        for (Enumeration e : enumerations) {
            enumNames.add(e.getName());
        }

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);

        for (Class cls : classes) {
            // classes abstratas não podem ser instanciadas, logo não têm CreateDTO/UpdateDTO
            if (!cls.isAbstract()) {
                generateCreateDTO(cls, associations, dtoDir);
                generateUpdateDTO(cls, associations, dtoDir);
            }
            generateResponseDTO(cls, associations, dtoDir);
        }

        System.out.println("DTO generation completed.");
    }

    /**
     * CreateDTO: only the class's own attributes (no id, no relationships)
     * Used for POST and PUT
     */
    private void generateCreateDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + className + "CreateDTO.java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            // relações navegáveis (mesmos campos de id que o ResponseDTO)
            List<RelationshipInfo> relationships = RelationshipInfo.collect(cls, associations);

            w.println("package " + basePackage + ".dto;");
            w.println();
            w.println("import lombok.*;");
            // java.util.* cobre List (ids de coleção) e UUID (chave natural ou id de relação)
            w.println("import java.util.*;");
            // atributos de data (LocalDate, etc.) precisam de java.time
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaTimeType(a.getType()))) {
                w.println("import java.time.*;");
            }
            w.println("import " + basePackage + ".models." + className + ";");

            // id gerado pela BD -> fora do CreateDTO
            // chave natural (String) é
            // atribuída pelo cliente -> mantém-se no CreateDTO
            // atributos com init -> fora: o init OBRIGA o valor na criação (semântica OCL);
            // ficam editáveis depois via UpdateDTO/PATCH
            String idName = cls.getRoot().getIdAttributeName();
            List<Attribute> allAttrs = cls.getAllAttributes().stream()
                    .filter(a -> !a.hasDerive())
                    .filter(a -> a.getInitExpression() == null)
                    .filter(a -> !cls.isHiddenInCreate(a.getName()))
                    .filter(a -> cls.isIdAssigned() || !a.getName().equals(idName))
                    .toList();

            // check if any attribute is an enum
            for (Attribute attr : allAttrs) {
                if (enumNames.contains(attr.getType())) {
                    w.println("import " + basePackage + ".models." + attr.getType() + ";");
                }
            }

            w.println();
            w.println("@Getter");
            w.println("@Setter");
            w.println("@NoArgsConstructor");
            if (!allAttrs.isEmpty() || !relationships.isEmpty()) {
                w.println("@AllArgsConstructor");
                w.println("@Builder");
            }
            w.println("public class " + className + "CreateDTO {");
            w.println();
            for (Attribute attr : allAttrs) {
                w.println("    private " + attr.getType() + " " + attr.getName() + ";");
            }

            // ids de relação (ligados no service, não no toEntity) — mesmo "emit" do ResponseDTO
            for (RelationshipInfo rel : relationships) {
                generateRelationshipIdField(w, rel);
            }

            // toEntity() method
            w.println();
            String varName = GeneratorUtils.lowercaseFirst(className);
            w.println("    public " + className + " toEntity() {");
            w.println("        " + className + " " + varName + " = new " + className + "();");
            // atributos com init nem aparecem aqui: o valor inicial fica com o default da entidade
            for (Attribute attr : allAttrs) {
                String cap = GeneratorUtils.capitalizeFirst(attr.getName());
                w.println("        " + varName + ".set" + cap + "(this.get" + cap + "());");
            }
            w.println("        return " + varName + ";");
            w.println("    }");

            w.println("}");
        }

        System.out.println("Generated: " + className + "CreateDTO.java");
    }

    /**
     * UpdateDTO: corpo do PATCH. Difere do CreateDTO em:
     * - exclui SEMPRE o atributo-id (a PK não muda em update, mesmo quando é chave natural);
     * - só ids de relação SINGULARES (coleções não se tocam no update — endpoints dedicados),
     *   por isso o campo xIds nem existe no contrato em vez de ser silenciosamente ignorado;
     * - sem toEntity(): o update mapeia campo-a-campo para a entidade já carregada.
     */
    private void generateUpdateDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + className + "UpdateDTO.java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            List<RelationshipInfo> relationships = RelationshipInfo.collectSingle(cls, associations);

            w.println("package " + basePackage + ".dto;");
            w.println();
            w.println("import lombok.*;");
            w.println("import java.util.*;");
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaTimeType(a.getType()))) {
                w.println("import java.time.*;");
            }

            String idName = cls.getRoot().getIdAttributeName();
            List<Attribute> allAttrs = cls.getAllAttributes().stream()
                    .filter(a -> !a.hasDerive())
                    .filter(a -> !cls.isHiddenInUpdate(a.getName()))
                    .filter(a -> !a.getName().equals(idName))
                    .toList();

            for (Attribute attr : allAttrs) {
                if (enumNames.contains(attr.getType())) {
                    w.println("import " + basePackage + ".models." + attr.getType() + ";");
                }
            }

            w.println();
            w.println("@Getter");
            w.println("@Setter");
            w.println("@NoArgsConstructor");
            if (!allAttrs.isEmpty() || !relationships.isEmpty()) {
                w.println("@AllArgsConstructor");
                w.println("@Builder");
            }
            w.println("public class " + className + "UpdateDTO {");
            w.println();
            for (Attribute attr : allAttrs) {
                // sempre com tipo fixed (Integer/Boolean/...), num PATCH um campo omitido chega a null e o
                // update ignora-o um primitivo chegaria 0/false, indistinguível de "omitido"
                w.println("    private " + GeneratorUtils.fixType(attr.getType()) + " " + attr.getName() + ";");
            }

            for (RelationshipInfo rel : relationships) {
                generateRelationshipIdField(w, rel);
            }

            w.println("}");
        }

        System.out.println("Generated: " + className + "UpdateDTO.java");
    }

    /**
     * ResponseDTO: id + attributes + relationship IDs
     * Used for GET responses and return values.
     * Além do default da classe, emite um DTO próprio por endpoint com override de response
     * (<Class><List|Detail|Create|Update>ResponseDTO) — mesma estrutura, atributos do override.
     */
    private void generateResponseDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        // esconde os hiddenInResponse (com herança)
        Set<String> defaultHidden = new HashSet<>();
        for (Attribute a : cls.getAllAttributes())
            if (cls.isHiddenInResponse(a.getName())) {
                defaultHidden.add(a.getName());
            }
        generateResponseDTOFile(cls, associations, dtoDir, cls.getName() + "ResponseDTO", defaultHidden);

        // overrides por endpoint (seleção independente do default); nome via responseDTOName
        // (custom se definido, senão auto), o mesmo que o controller/service usam
        for (String responseType : cls.getResponseOverrides().keySet()) {
            generateResponseDTOFile(cls, associations, dtoDir, responseDTOName(cls, responseType),
                    cls.responseOverrideHidden(responseType));
        }
    }

    // nome do response DTO que um endpoint usa: o do override do responseType, senão o default da classe.
    // o responseType já vem capitalizado (List/Get/Create/Update) -> nome auto por concatenação direta
    static String responseDTOName(Class cls, String responseType) {
        if (!cls.hasResponseOverride(responseType)) return cls.getName() + "ResponseDTO";
        String custom = cls.responseOverrideName(responseType);
        return !custom.isBlank() ? custom : cls.getName() + responseType + "ResponseDTO";
    }

    private void generateResponseDTOFile(Class cls, List<Association> associations, String dtoDir,
                                         String dtoName, Set<String> hiddenNames) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + dtoName + ".java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            w.println("package " + basePackage + ".dto;");
            w.println();
            w.println("import lombok.*;");
            w.println("import java.util.*;");
            w.println("import java.util.stream.Collectors;");
            // java.time.* cobre Instant (timestamps) e atributos de data (LocalDate, etc.)
            if (timestamps || cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaTimeType(a.getType()))) {
                w.println("import java.time.*;");
            }
            w.println("import " + basePackage + ".models." + className + ";");

            // import enums and related entity classes for fromEntity
            Set<String> extraImports = new LinkedHashSet<>();
            for (Attribute attr : cls.getAllAttributes()) {
                if (enumNames.contains(attr.getType())) {
                    extraImports.add(attr.getType());
                }
            }
            // entidades relacionadas para o fromEntity (.map(Other::getId)) — inclui
            // associações herdadas de pai abstrato, materializadas nesta subclasse
            for (Association assoc : associations) {
                String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
                if (declarer == null) continue;
                extraImports.add(GeneratorUtils.otherClassOf(declarer, assoc));
            }
            for (String imp : extraImports) {
                if (!imp.equals(className)) {
                    w.println("import " + basePackage + ".models." + imp + ";");
                }
            }

            w.println();
            w.println("@Getter");
            w.println("@Setter");
            w.println("@NoArgsConstructor");
            w.println("@AllArgsConstructor");
            w.println("@Builder");
            w.println("public class " + dtoName + " {");
            w.println();

            // id normal, quando há id custom, esse atributo já aparece nos atributos abaixo
            boolean defaultId = cls.getRoot().getIdAttributeName() == null;
            if (defaultId) {
                w.println("    private " + cls.getRoot().getIdAttributeType() + " id;");
                w.println();
            }

            // timestamps (geridos pelo servidor, só de leitura)
            if (timestamps) {
                w.println("    private Instant createdAt;");
                w.println("    private Instant updatedAt;");
                w.println();
            }

            // attributes (menos os escondidos deste DTO — mesma lista no fromEntity)
            List<Attribute> allAttrs = cls.getAllAttributes().stream()
                    .filter(a -> !hiddenNames.contains(a.getName()))
                    .toList();
            for (Attribute attr : allAttrs) {
                w.println("    private " + attr.getType() + " " + attr.getName() + ";");
            }

            // relationship ID fields (mesma lista usada nos campos e no fromEntity)
            List<RelationshipInfo> relationships = RelationshipInfo.collect(cls, associations);

            if (!relationships.isEmpty()) {
                w.println();
                for (RelationshipInfo rel : relationships) {
                    generateRelationshipIdField(w, rel);
                }
            }

            // fromEntity() static factory method
            w.println();
            String varName = GeneratorUtils.lowercaseFirst(className);
            w.println("    public static " + dtoName + " fromEntity(" + className + " " + varName + ") {");
            w.println("        " + dtoName + " dto = new " + dtoName + "();");
            if (defaultId) w.println("        dto.setId(" + varName + ".getId());");
            if (timestamps) {
                w.println("        dto.setCreatedAt(" + varName + ".getCreatedAt());");
                w.println("        dto.setUpdatedAt(" + varName + ".getUpdatedAt());");
            }

            for (Attribute attr : allAttrs) {
                String cap = GeneratorUtils.capitalizeFirst(attr.getName());
                w.println("        dto.set" + cap + "(" + varName + ".get" + cap + "());");
            }

            for (RelationshipInfo rel : relationships) {
                generateRelationshipIdMapping(w, varName, rel);
            }

            w.println("        return dto;");
            w.println("    }");

            w.println("}");
        }

        System.out.println("Generated: " + dtoName + ".java");
    }

    private void generateRelationshipIdField(PrintWriter w, RelationshipInfo rel) {
        String fieldName = GeneratorUtils.makeAttributeName(rel.otherClass, rel.role, rel.multiplicity);

        if (rel.isCollection()) {
            // collection -> List of related IDs
            w.println("    private List<" + idTypeOf(rel.otherClass) + "> " + fieldName + "Ids;");
        } else {
            // single reference -> related ID
            w.println("    private " + idTypeOf(rel.otherClass) + " " + fieldName + "Id;");
        }
    }

    private void generateRelationshipIdMapping(PrintWriter w, String varName, RelationshipInfo rel) {
        String otherClass = rel.otherClass;
        String fieldName = GeneratorUtils.makeAttributeName(otherClass, rel.role, rel.multiplicity);
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);

        if (rel.isCollection()) {
            if (rel.assoc.hasQualifier()) {
                w.println("        dto.set" + capitalizedField + "Ids(" + varName + ".get" + capitalizedField + "() != null");
                w.println("            ? " + varName + ".get" + capitalizedField + "().values().stream().map(" + otherClass + "::" + idGetterOf(otherClass) + ").collect(Collectors.toList())");
                w.println("            : new ArrayList<>());");
            } else {
                w.println("        dto.set" + capitalizedField + "Ids(" + varName + ".get" + capitalizedField + "() != null");
                w.println("            ? " + varName + ".get" + capitalizedField + "().stream().map(" + otherClass + "::" + idGetterOf(otherClass) + ").collect(Collectors.toList())");
                w.println("            : new ArrayList<>());");
            }
        } else {
            w.println("        dto.set" + capitalizedField + "Id(" + varName + ".get" + capitalizedField + "() != null");
            w.println("            ? " + varName + ".get" + capitalizedField + "()." + idGetterOf(otherClass) + "() : null);");
        }
    }

}
