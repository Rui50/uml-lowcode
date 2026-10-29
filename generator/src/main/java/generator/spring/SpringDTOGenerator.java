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
 * gerar os dtos, por classe
 * - CreateDTO: para POST, tem ids de relação (single e coleção)
 * - UpdateDTO: para PATCH e PUT só ids de relação SINGULARES
 * - ResponseDTO: para GET, relacionamento IDs
 */
public class SpringDTOGenerator {
    private String baseOutputPath;
    private String basePackage;
    private Set<String> enumNames = new HashSet<>();

    // createdAt/updatedAt no ResponseDTO
    private final boolean timestamps;

    // nome -> Class, para obter o tipo de id de outra classe nas relações
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
        String dtoDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/dto";
        new File(dtoDir).mkdirs();

        enumNames.clear();
        for (Enumeration e : enumerations) {
            enumNames.add(e.getName());
        }

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);

        for (Class cls : classes) {
            // cada classe tem a sua subpasta: dto/<classe>/
            String classDTODirectory = dtoDir + "/" + cls.getName().toLowerCase();
            new File(classDTODirectory).mkdirs();

            if (!cls.isAbstract()) {
                generateCreateDTO(cls, associations, classDTODirectory);
                generateUpdateDTO(cls, associations, classDTODirectory);
            }
            generateResponseDTO(cls, associations, classDTODirectory);
        }
    }

    /**
     * CreateDTO
     */
    private void generateCreateDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + className + "CreateDTO.java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            // relações navegáveis, menos as escondidas do Create
            List<RelationshipInfo> relationships = RelationshipInfo.collect(cls, associations).stream()
                    .filter(rel -> !cls.isHiddenInCreate(rel.attributeName()))
                    .toList();

            w.println("package " + basePackage + ".dto." + cls.getName().toLowerCase() + ";");
            w.println();
            w.println("import lombok.*;");
            // java.util.* cobre List (ids de coleção) e UUID (chave natural ou id de relação)
            w.println("import java.util.*;");
            // atributos de data (LocalDate, etc.) precisam de java.time
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaTimeType(a.getType()))) {
                w.println("import java.time.*;");
            }
            // atributos BigDecimal/BigInteger precisam de java.math
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaMathType(a.getType()))) {
                w.println("import java.math.*;");
            }
            w.println("import " + basePackage + ".models." + className + ";");

            // id gerado pela BD -> fora do CreateDTO
            // chave natural (String) é atribuída pelo cliente -> mantém-se no CreateDTO
            // atributos com init -> fora: o init manda no valor na criação
            // da para editar depois via UpdateDTO/PATCH
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
                // (Integer, não int) porque um campo omitido no POST chega a null, não a 0.
                String attributeType;
                if (attr.isCollection()) {
                    attributeType = attr.getAttributeType();
                } else {
                    attributeType = GeneratorUtils.fixType(attr.getType());
                }
                w.println("    private " + attributeType + " " + attr.getName() + ";");
            }

            // ids das relações ( authorId), a ligação real é feita no service não no toEntity()
            for (RelationshipInfo rel : relationships) {
                generateRelationshipIdAttribute(w, rel);
            }

            // toEntity() method
            w.println();
            String varName = GeneratorUtils.lowercaseFirst(className);
            w.println("    public " + className + " toEntity() {");
            w.println("        " + className + " " + varName + " = new " + className + "();");
            // atributos com init nem aparecem aqui, o valor inicial fica com o default da entidade
            for (Attribute attr : allAttrs) {
                String cap = GeneratorUtils.capitalizeFirst(attr.getName());
                // só copia se o cliente enviou o valor se veio null (omitido), deixa o default da entidade.
                w.println("        if (this.get" + cap + "() != null) " + varName + ".set" + cap + "(this.get" + cap + "());");
            }
            w.println("        return " + varName + ";");
            w.println("    }");

            w.println("}");
        }

        System.out.println("Generated: " + className + "CreateDTO.java");
    }

    /**
     * UpdateDTO
     * - só ids de relação sings (coleções não se tocam no update só nos endpoints dedicados),
     */
    private void generateUpdateDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + className + "UpdateDTO.java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            List<RelationshipInfo> relationships = RelationshipInfo.collectSingle(cls, associations).stream()
                    .filter(rel -> !cls.isHiddenInUpdate(rel.attributeName()))
                    .toList();

            w.println("package " + basePackage + ".dto." + cls.getName().toLowerCase() + ";");
            w.println();
            w.println("import lombok.*;");
            w.println("import java.util.*;");
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaTimeType(a.getType()))) {
                w.println("import java.time.*;");
            }
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaMathType(a.getType()))) {
                w.println("import java.math.*;");
            }
            w.println("import " + basePackage + ".models." + className + ";");

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
                // (Integer, não int) porque um campo omitido no PATCH chega a null, não a 0.
                String attributeType;
                if (attr.isCollection()) {
                    attributeType = attr.getAttributeType();
                } else {
                    attributeType = GeneratorUtils.fixType(attr.getType());
                }
                w.println("    private " + attributeType + " " + attr.getName() + ";");
            }

            for (RelationshipInfo rel : relationships) {
                generateRelationshipIdAttribute(w, rel);
            }

            // map de DTO para entidad
            // só atributos normais
            w.println();
            w.println("    // PATCH: só aplica os campos enviados (null não mexe)");
            w.println("    public void update(" + className + " e) {");
            for (Attribute attr : allAttrs) {
                String cap = GeneratorUtils.capitalizeFirst(attr.getName());
                w.println("        if (get" + cap + "() != null) e.set" + cap + "(get" + cap + "());");
            }
            w.println("    }");
            w.println();
            w.println("    // PUT: aplica todos os campos (o que vier null fica null)");
            w.println("    public void replace(" + className + " e) {");
            // primitivo omitido não pode ficar null (o setter da entidade recebe int/boolean), exige-se em vez de rebentar
            List<Attribute> primitives = allAttrs.stream()
                    .filter(a -> !a.isCollection() && GeneratorUtils.isPrimitive(a.getType())).toList();
            if (!primitives.isEmpty()) {
                w.println("        List<String> missing = new ArrayList<>();");
                for (Attribute attr : primitives) {
                    w.println("        if (get" + GeneratorUtils.capitalizeFirst(attr.getName())
                            + "() == null) missing.add(\"" + attr.getName() + "\");");
                }
                w.println("        if (!missing.isEmpty()) {");
                w.println("            throw new IllegalArgumentException(\"PUT requires all fields, missing: \""
                        + " + String.join(\", \", missing));");
                w.println("        }");
            }
            for (Attribute attr : allAttrs) {
                String cap = GeneratorUtils.capitalizeFirst(attr.getName());
                w.println("        e.set" + cap + "(get" + cap + "());");
            }
            w.println("    }");

            w.println("}");
        }

        System.out.println("Generated: " + className + "UpdateDTO.java");
    }

    /**
     * ResponseDTO: id + attributes + relationship IDs
     * Além do default da classe, emite um DTO próprio por endpoint com override de response
     * (<Class><List|Detail|Create|Update>ResponseDTO) — mesma estrutura, atributos do override.
     */
    private void generateResponseDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        // para o response default primeiro, esconde os hiddenInResponse
        List<Attribute> defaultAttrs = cls.getAllAttributes().stream()
                .filter(a -> !cls.isHiddenInResponse(a.getName())).toList();
        // collectForMenus e não collect: a resposta mostra também as relações herdadas, como já faz com os atributos
        List<RelationshipInfo> defaultRels = RelationshipInfo.collectForMenus(cls, associations).stream()
                .filter(r -> !cls.isHiddenInResponse(r.attributeName())).toList();
        generateResponseDTOFile(cls, dtoDir, cls.getName() + "ResponseDTO", defaultAttrs, defaultRels);

        // um DTO por override de endpoint
        for (String endpointType : cls.getResponseOverrides().keySet()) {
            Set<String> hidden = cls.responseOverrideHidden(endpointType);
            List<Attribute> attrs = cls.getAllAttributes().stream()
                    .filter(a -> !hidden.contains(a.getName())).toList();
            List<RelationshipInfo> rels = RelationshipInfo.collectForMenus(cls, associations).stream()
                    .filter(r -> !hidden.contains(r.attributeName())).toList();

            generateResponseDTOFile(cls, dtoDir, responseDTOName(cls, endpointType), attrs, rels);
        }
    }

    // nome do response DTO que um endpoint usa: o do override do endpointType, senão o default da classe.
    static String responseDTOName(Class cls, String endpointType) {
        if (!cls.hasResponseOverride(endpointType)) return cls.getName() + "ResponseDTO";
        String custom = cls.responseOverrideName(endpointType);
        // se o override do endpointType tiver nome custom, usa esse nome
        if (!custom.isBlank()) return custom;
        // senão usa o padrão <Class><endpointType>ResponseDTO
        return cls.getName() + endpointType + "ResponseDTO";
    }

    private void generateResponseDTOFile(Class cls, String dtoDir, String dtoName,
                                         List<Attribute> allAttrs, List<RelationshipInfo> relationships) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + dtoName + ".java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            w.println("package " + basePackage + ".dto." + cls.getName().toLowerCase() + ";");
            w.println();
            w.println("import lombok.*;");
            w.println("import java.util.*;");
            w.println("import java.util.stream.Collectors;");
            // java.time.* cobre Instant (timestamps) e atributos de data (LocalDate, etc.)
            if (timestamps || cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaTimeType(a.getType()))) {
                w.println("import java.time.*;");
            }
            // atributos BigDecimal/BigInteger precisam de java.math
            if (cls.getAllAttributes().stream().anyMatch(a -> GeneratorUtils.isJavaMathType(a.getType()))) {
                w.println("import java.math.*;");
            }
            w.println("import " + basePackage + ".models." + className + ";");

            // import enums and related entity classes for fromEntity
            Set<String> extraImports = new LinkedHashSet<>();
            for (Attribute attr : allAttrs) {
                if (enumNames.contains(attr.getType())) {
                    extraImports.add(attr.getType());
                }
            }
            // entidades relacionadas para o fromEntity (.map(Other::getId)), só as visíveis neste DTO
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
            // @AllArgsConstructor só se houver campos, com 0 campos gera um no arg que colide com @NoArgsConstructor.
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
                w.println("    private " + attr.getAttributeType() + " " + attr.getName() + ";");
            }

            // relationship ID fields
            if (!relationships.isEmpty()) {
                w.println();
                for (RelationshipInfo rel : relationships) {
                    generateRelationshipIdAttribute(w, rel);
                }
            }

            // fromEntity()
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
                if (attr.isCollection()) {
                    String container = attr.getCollectionType().equals("List") ? "ArrayList" : "HashSet";
                    w.println("        dto.set" + cap + "(" + varName + ".get" + cap + "() != null ? new " + container + "<>(" + varName + ".get" + cap + "()) : new " + container + "<>());");
                } else {
                    w.println("        dto.set" + cap + "(" + varName + ".get" + cap + "());");
                }
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

    private void generateRelationshipIdAttribute(PrintWriter w, RelationshipInfo rel) {
        String attributeName = rel.attributeName();

        if (rel.isCollection()) {
            // collection -> List of related IDs
            w.println("    private List<" + idTypeOf(rel.otherClass) + "> " + attributeName + "Ids;");
        } else {
            // single reference -> related ID
            w.println("    private " + idTypeOf(rel.otherClass) + " " + attributeName + "Id;");
        }
    }

    private void generateRelationshipIdMapping(PrintWriter w, String varName, RelationshipInfo rel) {
        String otherClass = rel.otherClass;
        String attributeName = rel.attributeName();
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);

        if (rel.isCollection()) {
            // .values() só se esta ponta for o Map
            if (rel.isMap()) {
                w.println("        dto.set" + capitalizedAttribute + "Ids(" + varName + ".get" + capitalizedAttribute + "() != null");
                w.println("            ? " + varName + ".get" + capitalizedAttribute + "().values().stream().map(" + otherClass + "::" + idGetterOf(otherClass) + ").collect(Collectors.toList())");
                w.println("            : new ArrayList<>());");
            } else {
                w.println("        dto.set" + capitalizedAttribute + "Ids(" + varName + ".get" + capitalizedAttribute + "() != null");
                w.println("            ? " + varName + ".get" + capitalizedAttribute + "().stream().map(" + otherClass + "::" + idGetterOf(otherClass) + ").collect(Collectors.toList())");
                w.println("            : new ArrayList<>());");
            }
        } else {
            w.println("        dto.set" + capitalizedAttribute + "Id(" + varName + ".get" + capitalizedAttribute + "() != null");
            w.println("            ? " + varName + ".get" + capitalizedAttribute + "()." + idGetterOf(otherClass) + "() : null);");
        }
    }

}
