package generator.CLI;

import generator.ValidationGenerator;
import model.Association;
import model.Attribute;
import model.Class;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class JavaEntityGenerator {
    private String baseOutputPath;
    private ValidationGenerator validationGenerator;

    public JavaEntityGenerator(String baseOutputPath) {
        this.baseOutputPath = baseOutputPath;
    }

    public void generateEntities(List<Class> classes, List<Association> associations) throws IOException {
        System.out.println("Generating java entities");

        String modelsPath = baseOutputPath + "/src/models";
        new File(modelsPath).mkdirs();

        this.validationGenerator = new ValidationGenerator(associations);

        for (Class cls : classes) {
            generateEntity(cls, classes, associations, modelsPath);
        }

        System.out.println("Java entity generation completed.");
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

            writer.println("}");
        }
    }

    private void generateAttributes(PrintWriter writer, Class cls) {
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasInit()) {
                String javaInit = translateOclToJava(attr.getInitExpression());
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

