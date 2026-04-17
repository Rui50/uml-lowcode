package generator.CLI;

import generator.ValidationGenerator;
import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.sequence.Interaction;
import model.Operation;
import model.Parameter;
import model.sequence.Interaction;
import generator.OperationGenerator;
import generator.GeneratorUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaEntityGenerator {
    private String baseOutputPath;
    private ValidationGenerator validationGenerator;
    private List<Interaction> interactions;
    private List<Enumeration> enumerations;
    // nomes das enumerações para saber quando um atributo é enum
    private Set<String> enumNames = new HashSet<>();

    public JavaEntityGenerator(String baseOutputPath, List<Interaction> interactions, List<Enumeration> enumerations) {
        this.baseOutputPath = baseOutputPath;
        this.interactions = interactions;
        this.enumerations = enumerations;
        if (enumerations != null) {
            for (Enumeration e : enumerations) {
                enumNames.add(e.getName());
            }
        }
    }

    public void generateEntities(List<Class> classes, List<Association> associations) throws IOException {
        generateEntities(classes, associations, new ArrayList<>());
    }

    public void generateEntities(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating java entities");

        String modelsPath = baseOutputPath + "/src/models";
        new File(modelsPath).mkdirs();

        this.validationGenerator = new ValidationGenerator(associations);

        for (Enumeration enumeration : enumerations) {
            generateEnum(enumeration, modelsPath);
        }

        for (Class cls : classes) {
            generateEntity(cls, classes, associations, modelsPath);
        }

        System.out.println("Java entity generation completed.");
    }

    private void generateEnum(Enumeration enumeration, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + enumeration.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            writer.println("package models;");
            writer.println();
            writer.println("public enum " + enumeration.getName() + " {");

            List<String> literals = enumeration.getLiterals();
            for (int i = 0; i < literals.size(); i++) {
                String separator = (i < literals.size() - 1) ? "," : ";";
                writer.println("    " + literals.get(i) + separator);
            }

            writer.println("}");

            System.out.println("Generated enum: " + enumeration.getName() + ".java");
        }
    }

    private void generateEntity(Class cls, List<Class> allClasses, List<Association> associations, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + cls.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(filename)) {
            // package
            writer.println("package models;");
            writer.println();

            // imports
            writer.println("import java.util.*;");
            writer.println();

            // class declaration
            writer.print("public class " + cls.getName());
            if (cls.getParentClass() != null) {
                writer.print(" extends " + cls.getParentClass().getName());
            }
            writer.println(" {");
            writer.println();

            // id (only if no parent)
            if (cls.getParentClass() == null) {
                writer.println("    private Long id;");
                writer.println();
            }

            // attributes
            generateAttributes(writer, cls);

            // relationships
            generateRelationships(writer, cls, associations);

            // empty constructor
            writer.println("    public " + cls.getName() + "() {}");
            writer.println();

            // constructor with attributes
            generateConstructor(writer, cls);

            // getters/setters for attributes
            if (cls.getParentClass() == null) {
                generateGetterSetter(writer, "Long", "id");
            }
            for (Attribute attr : cls.getAttributes()) {
                generateGetterSetter(writer, attr.getType(), attr.getName());
            }

            generateRelationshipGettersSetters(writer, cls, associations);

            // toSring
            generateToString(writer, cls);

            // validation
            if (!cls.getInvariants().isEmpty()) {
                validationGenerator.generateValidateMethod(writer, cls);
            }

            generateOperations(writer, cls, associations);

            writer.println("}");
        }
    }

    private void generateAttributes(PrintWriter writer, Class cls) {
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasInit()) {
                String javaInit = translateOclToJava(attr.getInitExpression());
                // se o tipo é um enum, meter com o nome do enum como prefixo (ATIVO -> EstadoAluno.ATIVO)
                if (enumNames.contains(attr.getType())) {
                    javaInit = attr.getType() + "." + javaInit;
                }
                writer.println("    private " + attr.getType() + " " + attr.getName() + " = " + javaInit + ";");
            } else {
                writer.println("    private " + attr.getType() + " " + attr.getName() + ";");
            }
        }
        if (!cls.getAttributes().isEmpty()) writer.println();
    }

    private String translateOclToJava(String oclExpression) {
        try {
            ocl.OCLLexer lexer = new ocl.OCLLexer(org.antlr.v4.runtime.CharStreams.fromString(oclExpression));
            ocl.OCLParser parser = new ocl.OCLParser(new org.antlr.v4.runtime.CommonTokenStream(lexer));
            return new ocl.OCLToJavaVisitor().visit(parser.standaloneExpression());
        } catch (Exception e) {
            System.err.println("Warning: Could not translate init expression: " + oclExpression);
            return oclExpression;
        }
    }

    private void generateRelationships(PrintWriter writer, Class cls, List<Association> associations) {
        List<String> generatedFields = new ArrayList<>();

        for (Association assoc : associations) {
            // source side
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String fieldName = makeFieldName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (!generatedFields.contains(fieldName)) {
                    generateRelationshipField(writer, assoc.getTargetClass(), fieldName, assoc.getTargetMultiplicity(), assoc);
                    generatedFields.add(fieldName);
                }
            }
            // target side
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String fieldName = makeFieldName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity());

                if (!generatedFields.contains(fieldName)) {
                    generateRelationshipField(writer, assoc.getSourceClass(), fieldName, assoc.getSourceMultiplicity(), assoc);
                    generatedFields.add(fieldName);
                }
            }
        }
    }

    private void generateConstructor(PrintWriter writer, Class cls) {
        // pegar nos atributos do pai
        List<Attribute> parentAttrs = new ArrayList<>();
        if (cls.getParentClass() != null) {
            parentAttrs = cls.getParentClass().getAttributes();
        }
        List<Attribute> ownAttrs = cls.getAttributes();

        // se não tem atributos nenhuns, não gera
        if (parentAttrs.isEmpty() && ownAttrs.isEmpty()) return;

        // construtor com atributos do pai + próprios
        writer.print("    public " + cls.getName() + "(");
        boolean first = true;
        for (Attribute attr : parentAttrs) {
            if (!first) writer.print(", ");
            writer.print(attr.getType() + " " + attr.getName());
            first = false;
        }
        for (Attribute attr : ownAttrs) {
            if (!first) writer.print(", ");
            writer.print(attr.getType() + " " + attr.getName());
            first = false;
        }
        writer.println(") {");

        // super() com atributos do pai
        if (!parentAttrs.isEmpty()) {
            writer.print("        super(");
            for (int i = 0; i < parentAttrs.size(); i++) {
                if (i > 0) writer.print(", ");
                writer.print(parentAttrs.get(i).getName());
            }
            writer.println(");");
        }

        // atributos próprios
        for (Attribute attr : ownAttrs) {
            writer.println("        this." + attr.getName() + " = " + attr.getName() + ";");
        }
        writer.println("    }");
        writer.println();
    }

    private void generateGetterSetter(PrintWriter writer, String type, String name) {
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);

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

    private void generateRelationshipGettersSetters(PrintWriter writer, Class cls, List<Association>
            associations) {
        List<String> generatedFields = new ArrayList<>();

        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String fieldName = makeFieldName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (!generatedFields.contains(fieldName)) {
                    String type = getRelationshipType(assoc.getTargetClass(), assoc.getTargetMultiplicity(), assoc);
                    generateGetterSetter(writer, type, fieldName);
                    generatedFields.add(fieldName);
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String fieldName = makeFieldName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity());

                if (!generatedFields.contains(fieldName)) {
                    String type = getRelationshipType(assoc.getSourceClass(), assoc.getSourceMultiplicity(), assoc);
                    generateGetterSetter(writer, type, fieldName);
                    generatedFields.add(fieldName);
                }
            }
        }
    }

    private void generateOperations(PrintWriter writer, Class cls, List<Association> associations) {
        if (cls.getOperations().isEmpty()) return;

        writer.println("    // Operations");

        for (Operation op : cls.getOperations()) {
            Interaction interaction = OperationGenerator.findInteractionForOperation(cls, op, interactions);
            if (interaction != null) {
                OperationGenerator.generateOperation(writer, cls, interaction, associations);
            } else {
                String visibility = op.getVisibility();
                String returnType = GeneratorUtils.removePackageNames(op.getReturnType());
                writer.println("    " + visibility + " " + returnType + " " + op.getName() + "(");

                List<Parameter> parameters = op.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    Parameter param = parameters.get(i);
                    String paramType = GeneratorUtils.removePackageNames(param.getType());
                    writer.print("        " + paramType + " " + param.getName());
                    writer.println(i < parameters.size() - 1 ? "," : "");
                }

                writer.println("    ) {");
                writer.println("        throw new UnsupportedOperationException(\"Not implemented yet\");");
                writer.println("    }");
            }
        }
    }

    private void generateToString(PrintWriter writer, Class cls) {
        writer.println("    @Override");
        writer.println("    public String toString() {");
        writer.print("        return \"" + cls.getName() + "{\" +");
        writer.println();
        writer.print("            \"id=\" + getId()");

        for (Attribute attr : cls.getAttributes()) {
            writer.println(" +");
            if (attr.getType().equals("String")) {
                writer.print("            \", " + attr.getName() + "='\" + " + attr.getName() + " +" +
                        "\"'\"");
            } else {
                writer.print("            \", " + attr.getName() + "=\" + " + attr.getName());
            }
        }

        writer.println(" +");
        writer.println("            \"}\";");
        writer.println("    }");
        writer.println();
    }

    private void generateRelationshipField(PrintWriter writer, String otherClass, String fieldName, String multiplicity, Association assoc) {
        if (assoc.hasQualifier()) {
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " + fieldName + " = new HashMap<>();");
        } else if (isMultiple(multiplicity)) {
            writer.println("    private List<" + otherClass + "> " + fieldName + " = new ArrayList<>();");
        } else {
            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }

    private String makeFieldName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }
        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        return isMultiple(multiplicity) ? base + "s" : base;
    }

    private String getRelationshipType(String otherClass, String multiplicity, Association assoc) {
        if (assoc.hasQualifier()) {
            return "Map<" + assoc.getQualifierType() + ", " + otherClass + ">";
        } else if (isMultiple(multiplicity)) {
            return "List<" + otherClass + ">";
        } else {
            return otherClass;
        }
    }

    private boolean isMultiple(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }
}

