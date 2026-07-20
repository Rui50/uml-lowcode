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
            // relações navegáveis (mesmos campos de id que o ResponseDTO), menos as escondidas do Create
            List<RelationshipInfo> relationships = RelationshipInfo.collect(cls, associations).stream()
                    .filter(rel -> !cls.isHiddenInCreate(rel.field()))
                    .toList();

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
                // tipo boxed (Integer/Boolean/...) omitido no POST chega a null e o toEntity() ignora-o, em vez
                // de um primitivo chegar 0/false (valor inventado).
                w.println("    private " + GeneratorUtils.fixType(attr.getType()) + " " + attr.getName() + ";");
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
                // o campo do DTO é boxed (vem null se omitido) mas a entidade é PRIMITIVA. setX(this.getX())
                // auto-tira o Integer para int -> NPE se null. O guard evita isso: omitido -> não mexe,
                // fica o default da entidade em vez de um 0/false inventado.
                w.println("        if (this.get" + cap + "() != null) " + varName + ".set" + cap + "(this.get" + cap + "());");
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
            List<RelationshipInfo> relationships = RelationshipInfo.collectSingle(cls, associations).stream()
                    .filter(rel -> !cls.isHiddenInUpdate(rel.field()))
                    .toList();

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
        // para o response DEFAULT primeiro, esconde os hiddenInResponse (com herança)
        List<Attribute> defaultAttrs = cls.getAllAttributes().stream()
                .filter(a -> !cls.isHiddenInResponse(a.getName())).toList();
        List<RelationshipInfo> defaultRels = RelationshipInfo.collect(cls, associations).stream()
                .filter(r -> !cls.isHiddenInResponse(r.field())).toList();
        generateResponseDTOFile(cls, dtoDir, cls.getName() + "ResponseDTO", defaultAttrs, defaultRels);

        // um DTO por override de endpoint (não herda). O nome via
        // responseDTOName (custom se definido, senão auto) é o mesmo que o controller/service usam.
        for (String endpointType : cls.getResponseOverrides().keySet()) {
            Set<String> hidden = cls.responseOverrideHidden(endpointType);
            List<Attribute> attrs = cls.getAllAttributes().stream()
                    .filter(a -> !hidden.contains(a.getName())).toList();
            List<RelationshipInfo> rels = RelationshipInfo.collect(cls, associations).stream()
                    .filter(r -> !hidden.contains(r.field())).toList();

            generateResponseDTOFile(cls, dtoDir, responseDTOName(cls, endpointType), attrs, rels);
        }
    }

    // nome do response DTO que um endpoint usa: o do override do endpointType, senão o default da classe.
    // o endpointType já vem capitalizado (List/Get/Create/Update) -> nome auto por concatenação direta
    static String responseDTOName(Class cls, String endpointType) {
        if (!cls.hasResponseOverride(endpointType)) return cls.getName() + "ResponseDTO";
        String custom = cls.responseOverrideName(endpointType);
        return !custom.isBlank() ? custom : cls.getName() + endpointType + "ResponseDTO";
    }

    private void generateResponseDTOFile(Class cls, String dtoDir, String dtoName,
                                         List<Attribute> allAttrs, List<RelationshipInfo> relationships) throws IOException {
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
            for (Attribute attr : allAttrs) {
                if (enumNames.contains(attr.getType())) {
                    extraImports.add(attr.getType());
                }
            }
            // entidades relacionadas para o fromEntity (.map(Other::getId)) — só as visíveis neste DTO
            for (RelationshipInfo rel : relationships) {
                extraImports.add(rel.otherClass);
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
            // @AllArgsConstructor sobre 0 campos gera um no-arg -> colide com @NoArgsConstructor.
            // ResponseDTO pode ficar sem campos (id custom + tudo escondido no response) -> guardar.
            boolean defaultId = cls.getRoot().getIdAttributeName() == null;
            if (defaultId || timestamps || !allAttrs.isEmpty() || !relationships.isEmpty()) {
                w.println("@AllArgsConstructor");
                w.println("@Builder");
            }
            w.println("public class " + dtoName + " {");
            w.println();

            // id normal, quando há id custom, esse atributo já aparece nos atributos abaixo
            // (defaultId já calculado acima)
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

            // attributes ity)
            for (Attribute attr : allAttrs) {
                w.println("    private " + attr.getType() + " " + attr.getName() + ";");
            }

            // relationship ID fields
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
        String fieldName = rel.field();

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
        String fieldName = rel.field();
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);

        if (rel.isCollection()) {
            // .values() só se ESTA ponta for o Map; numa M:N a outra ponta é coleção na mesma (List)
            if (rel.assoc.isMapEnd(rel.isSource)) {
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
