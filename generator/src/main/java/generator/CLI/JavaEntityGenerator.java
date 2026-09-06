package generator.CLI;

import generator.ValidationGenerator;
import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.sequence.Interaction;
import model.Operation;
import model.Parameter;
import generator.OperationGenerator;
import generator.GeneratorUtils;
import generator.RelationshipInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * gera as entidades para o CLI (Java simples nao tem JPA)
 */
public class JavaEntityGenerator {
    private String baseOutputPath;
    private ValidationGenerator validationGenerator;
    private List<Interaction> interactions;

    /**
     * Para ja
     *  - todas as classes sem pai recebem um campo Long id
     *  - atributos com init OCL são inicializados no construtor
     *  - gera construtores
     *  - gera toString()
     */
    private String basePackage;

    public JavaEntityGenerator(String baseOutputPath, String basePackage, List<Interaction> interactions) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
        this.interactions = interactions;
    }

    public void generateEntities(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating java entities");

        String modelsPath = baseOutputPath + "/src/" + basePackage.replace('.', '/') + "/models";
        new File(modelsPath).mkdirs();

        this.validationGenerator = new ValidationGenerator();

        for (Enumeration enumeration : enumerations) {
            generateEnum(enumeration, modelsPath);
        }

        for (Class cls : classes) {
            generateEntity(cls, classes, associations, modelsPath);
        }

        System.out.println("Java entity generation completed.");
    }

    /**
     * gera um ficheiro Java por interface UML (só assinaturas).
     */
    public void generateInterfaces(List<model.Interface> interfaces) throws IOException {
        if (interfaces == null || interfaces.isEmpty()) return;

        String modelsPath = baseOutputPath + "/src/" + basePackage.replace('.', '/') + "/models";
        new File(modelsPath).mkdirs();

        for (model.Interface iface : interfaces) {
            String filename = modelsPath + "/" + iface.getName() + ".java";
            try (PrintWriter w = new PrintWriter(filename)) {
                w.println("package " + basePackage + ".models;");
                w.println();
                w.println("public interface " + iface.getName() + " {");
                w.println();
                for (Operation op : iface.getOperations()) {
                    w.println("    " + GeneratorUtils.buildSignature(op) + ";");
                }
                w.println("}");
            }
            System.out.println("Generated interface: " + iface.getName() + ".java");
        }
    }

    // gera enum
    private void generateEnum(Enumeration enumeration, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + enumeration.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            writer.println("package " + basePackage + ".models;");
            writer.println();
            writer.println("public enum " + enumeration.getName() + " {");

            // cada elemento do enum
            List<String> literals = enumeration.getLiterals();
            for (int i = 0; i < literals.size(); i++) {
                String separator = (i < literals.size() - 1) ? "," : ";";
                writer.println("    " + literals.get(i) + separator);
            }

            writer.println("}");

            System.out.println("Generated enum: " + enumeration.getName() + ".java");
        }
    }

    // gera classe de entitdade, com imports, id, atributos, relaçoes,
    // construtores, getters/setters, toString, validação e operações
    private void generateEntity(Class cls, List<Class> allClasses, List<Association> associations, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + cls.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(filename)) {
            // package
            writer.println("package " + basePackage + ".models;");
            writer.println();

            // imports
            writer.println("import java.util.*;");
            writer.println("import java.time.*;");   // atributos de data (LocalDate, etc.)
            writer.println();

            // class declaration
            writer.print("public " + (cls.isAbstract() ? "abstract " : "") + "class " + cls.getName());
            if (cls.getParentClass() != null) {
                writer.print(" extends " + cls.getParentClass().getName());
            }
            // se implementa interface
            if (!cls.getRealizations().isEmpty()) {
                writer.print(" implements ");
                for (int i = 0; i < cls.getRealizations().size(); i++) {
                    if (i > 0) writer.print(", ");
                    writer.print(cls.getRealizations().get(i).getName());
                }
            }
            writer.println(" {");
            writer.println();

            // id
            if (cls.getParentClass() == null) {
                writer.println("    private Long id;");
                writer.println();
            }

            // attributes
            generateAttributes(writer, cls);

            // relationships
            generateRelationshipAttributes(writer, cls, associations);

            // construtores (no arg e completo)
            generateConstructor(writer, cls);

            // getters/setters for attributes
            if (cls.getParentClass() == null) {
                generateGetterSetter(writer, "Long", "id");
            }

            // se o atributo é derived, só gera getter com a expressão OCL, sem setter
            for (Attribute attr : cls.getAttributes()) {
                if (attr.hasDerive()) {
                    generateDerivedGetter(writer, attr);
                } else {
                    generateGetterSetter(writer, attr.getAttributeType(), attr.getName());
                }
            }

            generateRelationshipGettersSetters(writer, cls, associations);

            // toSring
            generateToString(writer, cls);

            // validation
            validationGenerator.generateValidateMethod(writer, cls);

            OperationGenerator.generateOperations(writer, cls, associations);

            writer.println("}");
        }
    }

    // gera os atributos nao derivados
    // para atributos de tipo enum, prefixa com o nome do enum
    private void generateAttributes(PrintWriter writer, Class cls) {
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasDerive()) continue;
            String line = "    private " + attr.getAttributeType() + " " + attr.getName();
            if (attr.isCollection()) {
                if (attr.getCollectionType().equals("List")) {
                    line += " = new ArrayList<>()";
                } else {
                    line += " = new HashSet<>()";
                }
            }
            writer.println(line + ";");
        }
        if (!cls.getAttributes().isEmpty()) writer.println();
    }

    // gera os campos para as relações navegáveis
    private void generateRelationshipAttributes(PrintWriter writer, Class cls, List<Association> associations) {
        // so para prevenir no caso de nao haver roles para nao duplicar atributos com o mesmo nome de classe
        List<String> generatedAttributes = new ArrayList<>();

        for (RelationshipInfo rel : RelationshipInfo.collectOwn(cls, associations)) {
            String attributeName = rel.attributeName();
            if (!generatedAttributes.contains(attributeName)) {
                generateRelationshipAttribute(writer, rel.otherClass, attributeName, rel.otherMultiplicity, rel.assoc, rel.isSource);
                generatedAttributes.add(attributeName);
            }
        }
    }

    private void generateConstructor(PrintWriter writer, Class cls) {
        // atributos herdados de pais (para o super(...))
        List<Attribute> parentAttrs = GeneratorUtils.ancestorAttributes(cls);

        // filter aos derives e inits só os que são parâmetros do construtor (this.x = x recebido)
        List<Attribute> paramParentAttrs = parentAttrs.stream()
                .filter(a -> !a.hasDerive() && !a.hasInit()).toList();

        // atributos próprios, sem derivados
        List<Attribute> ownAttrs = cls.getAttributes().stream()
                .filter(a -> !a.hasDerive()).toList();

        // sem inits
        List<Attribute> paramAttrs = ownAttrs.stream()
                .filter(a -> !a.hasInit()).toList();

        // os que têm init OCL (this.x = expr)
        List<Attribute> initAttrs = ownAttrs.stream()
                .filter(Attribute::hasInit).toList();

        boolean noParams = paramParentAttrs.isEmpty() && paramAttrs.isEmpty();

        // construtor no arg existe sempre, mas tem os inits
        writer.println("    public " + cls.getName() + "() {");
        for (Attribute attr : initAttrs) {
            writer.println("        this." + attr.getName() + " = " + attr.getJavaInitExpression() + ";");
        }
        writer.println("    }");
        writer.println();

        // sem parâmetros, o no arg é o único construtor
        if (noParams) return;

        // construtor completo se tiver params
        writer.print("    public " + cls.getName() + "(");
        boolean first = true;
        // atributos herdados
        for (Attribute attr : paramParentAttrs) {
            if (!first) writer.print(", ");
            writer.print(attr.getAttributeType() + " " + attr.getName());
            first = false;
        }
        // atributos próprios
        for (Attribute attr : paramAttrs) {
            if (!first) writer.print(", ");
            writer.print(attr.getAttributeType() + " " + attr.getName());
            first = false;
        }
        writer.println(") {");

        // super() com atributos do pai (sem init)
        if (!paramParentAttrs.isEmpty()) {
            writer.print("        super(");
            for (int i = 0; i < paramParentAttrs.size(); i++) {
                if (i > 0) writer.print(", ");
                writer.print(paramParentAttrs.get(i).getName());
            }
            writer.println(");");
        }

        // atribui os params próprios (this.x = x recebido)
        for (Attribute attr : paramAttrs) {
            writer.println("        this." + attr.getName() + " = " + attr.getName() + ";");
        }

        // inits OCL
        for (Attribute attr : initAttrs) {
            writer.println("        this." + attr.getName() + " = " + attr.getJavaInitExpression() + ";");
        }

        writer.println("    }");
        writer.println();
    }


    private void generateGetterSetter(PrintWriter writer, String type, String name) {
        String capitalized = GeneratorUtils.capitalizeFirst(name);

        // getter
        writer.println("    public " + type + " get" + capitalized + "() {");
        writer.println("        return " + name + ";");
        writer.println("    }");
        writer.println();

        // setter
        writer.println("    public void set" + capitalized + "(" + type + " " + name + ") {");
        writer.println("        this." + name + " = " + name + ";");
        writer.println("    }");
        writer.println();
    }

    private void generateDerivedGetter(PrintWriter writer, Attribute attr) {
        String name = attr.getName();
        String capitalized = GeneratorUtils.capitalizeFirst(name);
        writer.println("    public " + attr.getType() + " get" + capitalized + "() {");
        writer.println("        return " + attr.getJavaDeriveExpression() + ";");
        writer.println("    }");
        writer.println();
    }

    private void generateRelationshipGettersSetters(PrintWriter writer, Class cls, List<Association>
            associations) {
        List<String> generatedAttributes = new ArrayList<>();

        for (RelationshipInfo rel : RelationshipInfo.collectOwn(cls, associations)) {
            String attributeName = rel.attributeName();
            if (!generatedAttributes.contains(attributeName)) {
                String type = getRelationshipType(rel.otherClass, rel.otherMultiplicity, rel.assoc, rel.isSource);
                generateGetterSetter(writer, type, attributeName);
                generatedAttributes.add(attributeName);
            }
        }
    }


    // gera um metodo toString para a classe
    private void generateToString(PrintWriter writer, Class cls) {
        writer.println("    @Override");
        writer.println("    public String toString() {");
        writer.print("        return \"" + cls.getName() + "{\" +");
        writer.println();
        writer.print("            \"id=\" + getId()");

        for (Attribute attr : cls.getAllAttributes()) {
            writer.println(" +");
            String capitalized = GeneratorUtils.capitalizeFirst(attr.getName());
            String expr = "get" + capitalized + "()";
            if (!attr.isCollection() && attr.getType().equals("String")) {
                writer.print("            \", " + attr.getName() + "='\" + " + expr + " + \"'\"");
            } else {
                writer.print("            \", " + attr.getName() + "=\" + " + expr);
            }
        }

        writer.println(" +");
        writer.println("            \"}\";");
        writer.println("    }");
        writer.println();
    }

    // gera o campo para uma relação que pode ser Map (com qualifier), List ou referência simples
    private void generateRelationshipAttribute(PrintWriter writer, String otherClass, String attributeName, String multiplicity, Association assoc, boolean isSource) {
        // so a ponta que tem o qualifier é Map, a outra é referência normal
        if (assoc.isMapEnd(isSource)) {
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " + attributeName + " = new HashMap<>();");
        } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
            writer.println("    private List<" + otherClass + "> " + attributeName + " = new ArrayList<>();");
        } else {
            writer.println("    private " + otherClass + " " + attributeName + ";");
        }
    }

    // tipo da relação (Map/List/ref) para os gets sets
    private String getRelationshipType(String otherClass, String multiplicity, Association assoc, boolean isSource) {
        if (assoc.isMapEnd(isSource)) {
            return "Map<" + assoc.getQualifierType() + ", " + otherClass + ">";
        } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
            return "List<" + otherClass + ">";
        } else {
            return otherClass;
        }
    }

}

