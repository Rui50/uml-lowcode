package generator.spring;

import generator.GeneratorUtils;
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
 * - CreateDTO: para POST/PUT. apenas atributos, sem id, relações
 * - ResponseDTO: para GET. id + atributos + relacionamento IDs
 *
 * id: CreateDTO exclui sempre o atributo-id (a BD atribui-o);
 * ResponseDTO expõe 'id' por defeito só quando não há id custom; relações usam o tipo/getter do id relacionado. Ver model.Class.
 */
public class SpringDTOGenerator {
    private String baseOutputPath;
    private String basePackage;
    private Set<String> enumNames = new HashSet<>();

    // nome -> Class, para resolver o tipo de id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    public SpringDTOGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    // tipo do id de uma classe (pelo nome), resolvido a partir da raiz; default Long
    private String idTypeOf(String className) {
        Class c = classMap.get(className);
        return (c != null) ? c.getRoot().getIdAttributeType() : "Long";
    }

    // getter do id
    private String idGetterOf(String className) {
        Class c = classMap.get(className);
        String field = (c != null && c.getRoot().getIdAttributeName() != null)
                ? c.getRoot().getIdAttributeName() : "id";
        return "get" + GeneratorUtils.capitalizeFirst(field);
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
            // classes abstratas não podem ser instanciadas, logo não têm CreateDTO
            if (!cls.isAbstract()) generateCreateDTO(cls, associations, dtoDir);
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
            w.println("package " + basePackage + ".dto;");
            w.println();
            w.println("import lombok.*;");
            w.println("import " + basePackage + ".models." + className + ";");

            // id é sempre auto-gerado pela BD -> fora do CreateDTO
            String idName = cls.getRoot().getIdAttributeName();
            List<Attribute> allAttrs = cls.getAllAttributes().stream()
                    .filter(a -> !a.hasDerive())
                    .filter(a -> !a.getName().equals(idName))
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
            if (!allAttrs.isEmpty()) {
                w.println("@AllArgsConstructor");
                w.println("@Builder");
            }
            w.println("public class " + className + "CreateDTO {");
            w.println();
            for (Attribute attr : allAttrs) {
                w.println("    private " + attr.getType() + " " + attr.getName() + ";");
            }

            // toEntity() method
            w.println();
            String varName = className.substring(0, 1).toLowerCase() + className.substring(1);
            w.println("    public " + className + " toEntity() {");
            w.println("        " + className + " " + varName + " = new " + className + "();");
            for (Attribute attr : allAttrs) {
                String cap = attr.getName().substring(0, 1).toUpperCase() + attr.getName().substring(1);
                String prefix = "boolean".equals(attr.getType()) ? "is" : "get";
                if (attr.getInitExpression() != null) {
                    // preserve entity's init default when client didn't send a value
                    w.println("        if (this." + prefix + cap + "() != null) " + varName + ".set" + cap + "(this." + prefix + cap + "());");
                } else {
                    w.println("        " + varName + ".set" + cap + "(this." + prefix + cap + "());");
                }
            }
            w.println("        return " + varName + ";");
            w.println("    }");

            w.println("}");
        }

        System.out.println("Generated: " + className + "CreateDTO.java");
    }

    /**
     * ResponseDTO: id + attributes + relationship IDs
     * Used for GET responses and return values
     */
    private void generateResponseDTO(Class cls, List<Association> associations, String dtoDir) throws IOException {
        String className = cls.getName();
        String filename = dtoDir + "/" + className + "ResponseDTO.java";

        try (PrintWriter w = new PrintWriter(new File(filename))) {
            w.println("package " + basePackage + ".dto;");
            w.println();
            w.println("import lombok.*;");
            w.println("import java.util.*;");
            w.println("import java.util.stream.Collectors;");
            w.println("import " + basePackage + ".models." + className + ";");

            // import enums and related entity classes for fromEntity
            Set<String> extraImports = new LinkedHashSet<>();
            for (Attribute attr : cls.getAllAttributes()) {
                if (enumNames.contains(attr.getType())) {
                    extraImports.add(attr.getType());
                }
            }
            for (Association assoc : associations) {
                if (assoc.getSourceClass().equals(className) && assoc.isTargetNavigable()) {
                    extraImports.add(assoc.getTargetClass());
                }
                if (assoc.getTargetClass().equals(className) && assoc.isSourceNavigable()) {
                    extraImports.add(assoc.getSourceClass());
                }
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
            w.println("public class " + className + "ResponseDTO {");
            w.println();

            // id normal, quando há id custom, esse atributo já aparece nos atributos abaixo
            boolean defaultId = cls.getRoot().getIdAttributeName() == null;
            if (defaultId) {
                w.println("    private Long id;");
                w.println();
            }

            // attributes
            List<Attribute> allAttrs = cls.getAllAttributes();
            for (Attribute attr : allAttrs) {
                w.println("    private " + attr.getType() + " " + attr.getName() + ";");
            }

            // relationship ID fields
            // collect relationship info for both fields and fromEntity mapping
            List<RelationshipInfo> relationships = new ArrayList<>();
            for (Association assoc : associations) {
                if (assoc.getSourceClass().equals(className) && assoc.isTargetNavigable()) {
                    relationships.add(new RelationshipInfo(
                            assoc.getTargetClass(), assoc.getTargetRole(),
                            assoc.getTargetMultiplicity(), assoc));
                }
                if (assoc.getTargetClass().equals(className) && assoc.isSourceNavigable()) {
                    relationships.add(new RelationshipInfo(
                            assoc.getSourceClass(), assoc.getSourceRole(),
                            assoc.getSourceMultiplicity(), assoc));
                }
            }

            if (!relationships.isEmpty()) {
                w.println();
                for (RelationshipInfo rel : relationships) {
                    generateRelationshipIdField(w, rel.otherClass, rel.role, rel.multiplicity, rel.assoc);
                }
            }

            // fromEntity() static factory method
            w.println();
            String varName = className.substring(0, 1).toLowerCase() + className.substring(1);
            w.println("    public static " + className + "ResponseDTO fromEntity(" + className + " " + varName + ") {");
            w.println("        " + className + "ResponseDTO dto = new " + className + "ResponseDTO();");
            if (defaultId) w.println("        dto.setId(" + varName + ".getId());");

            for (Attribute attr : allAttrs) {
                String cap = attr.getName().substring(0, 1).toUpperCase() + attr.getName().substring(1);
                String prefix = "boolean".equals(attr.getType()) ? "is" : "get";
                w.println("        dto.set" + cap + "(" + varName + "." + prefix + cap + "());");
            }

            for (RelationshipInfo rel : relationships) {
                generateRelationshipIdMapping(w, varName, rel.otherClass, rel.role, rel.multiplicity, rel.assoc);
            }

            w.println("        return dto;");
            w.println("    }");

            w.println("}");
        }

        System.out.println("Generated: " + className + "ResponseDTO.java");
    }

    private void generateRelationshipIdField(PrintWriter w, String otherClass, String role,
                                              String multiplicity, Association assoc) {
        String fieldName = GeneratorUtils.makeAttributeName(otherClass, role, multiplicity);

        if (GeneratorUtils.checkMultiplicity(multiplicity) || assoc.hasQualifier()) {
            // collection -> List of related IDs
            w.println("    private List<" + idTypeOf(otherClass) + "> " + fieldName + "Ids;");
        } else {
            // single reference -> related ID
            w.println("    private " + idTypeOf(otherClass) + " " + fieldName + "Id;");
        }
    }

    private void generateRelationshipIdMapping(PrintWriter w, String varName, String otherClass,
                                                String role, String multiplicity, Association assoc) {
        String fieldName = GeneratorUtils.makeAttributeName(otherClass, role, multiplicity);
        String capitalizedField = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

        if (GeneratorUtils.checkMultiplicity(multiplicity) || assoc.hasQualifier()) {
            if (assoc.hasQualifier()) {
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

    // helper to carry relationship info
    private static class RelationshipInfo {
        String otherClass, role, multiplicity;
        Association assoc;

        RelationshipInfo(String otherClass, String role, String multiplicity, Association assoc) {
            this.otherClass = otherClass;
            this.role = role;
            this.multiplicity = multiplicity;
            this.assoc = assoc;
        }
    }

}
