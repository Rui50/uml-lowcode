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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class JavaEntityGenerator {
    private String baseOutputPath;
    private ValidationGenerator validationGenerator;
    private List<Interaction> interactions;

    /**
     * Para ja
     *  - todas as classes sem pai recebem um campo Long id
     *  - atributos com init OCL têm inicialização inline (= valor)
     *  - tipos enum são prefixados com o nome do enum (ATIVO -> EstadoAluno.ATIVO)
     *  - gera construtor vazio + construtor com todos os atributos, herdado via super
     *  - gera toString()
     */
    public JavaEntityGenerator(String baseOutputPath, List<Interaction> interactions) {
        this.baseOutputPath = baseOutputPath;
        this.interactions = interactions;
    }

    public void generateEntities(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating java entities");

        String modelsPath = baseOutputPath + "/src/models";
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

        String modelsPath = baseOutputPath + "/src/models";
        new File(modelsPath).mkdirs();

        for (model.Interface iface : interfaces) {
            String filename = modelsPath + "/" + iface.getName() + ".java";
            try (PrintWriter w = new PrintWriter(filename)) {
                w.println("package models;");
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

    // gera classe de entitdade, com imports, id, atributos, relaçoes,
    // construtores, getters/setters, toString, validação e operações
    private void generateEntity(Class cls, List<Class> allClasses, List<Association> associations, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + cls.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(filename)) {
            // package
            writer.println("package models;");
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
            if (!cls.getRealizations().isEmpty()) {
                writer.print(" implements ");
                for (int i = 0; i < cls.getRealizations().size(); i++) {
                    if (i > 0) writer.print(", ");
                    writer.print(cls.getRealizations().get(i).getName());
                }
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
                if (attr.hasDerive()) {
                    generateDerivedGetter(writer, attr);
                } else {
                    generateGetterSetter(writer, attr.getType(), attr.getName());
                }
            }

            generateRelationshipGettersSetters(writer, cls, associations);

            // toSring
            generateToString(writer, cls);

            // validation
            validationGenerator.generateValidateMethod(writer, cls);

            generateOperations(writer, cls, associations);

            writer.println("}");
        }
    }

    // gera os atributos nao derivados
    // para atributos de tipo enum, prefixa com o nome do enum
    private void generateAttributes(PrintWriter writer, Class cls) {
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasDerive()) continue;
            writer.println("    private " + attr.getType() + " " + attr.getName() + ";");
        }
        if (!cls.getAttributes().isEmpty()) writer.println();
    }

    // gera os campos para as relações navegáveis
    private void generateRelationships(PrintWriter writer, Class cls, List<Association> associations) {
        List<String> generatedFields = new ArrayList<>();

        for (Association assoc : associations) {
            // source side
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String fieldName = GeneratorUtils.makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (!generatedFields.contains(fieldName)) {
                    generateRelationshipField(writer, assoc.getTargetClass(), fieldName, assoc.getTargetMultiplicity(), assoc);
                    generatedFields.add(fieldName);
                }
            }
            // target side
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String fieldName = GeneratorUtils.makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity());

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
        // só os params do pai (sem derivados nem init)
        List<Attribute> paramParentAttrs = parentAttrs.stream()
                .filter(a -> !a.hasDerive() && !a.hasInit()).toList();

        // filtrar os nao derivados
        List<Attribute> ownAttrs = cls.getAttributes().stream()
                .filter(a -> !a.hasDerive()).toList();

        // os que tem init e que nao
        List<Attribute> paramAttrs = ownAttrs.stream()
                .filter(a -> !a.hasInit()).toList();
        List<Attribute> initAttrs = ownAttrs.stream()
                .filter(Attribute::hasInit).toList();

        // se não tem atributos nenhuns, não gera
        if (parentAttrs.isEmpty() && ownAttrs.isEmpty()) return;

        // construtor com atributos do pai + próprios
        writer.print("    public " + cls.getName() + "(");
        boolean first = true;
        for (Attribute attr : paramParentAttrs) {
            if (!first) writer.print(", ");
            writer.print(attr.getType() + " " + attr.getName());
            first = false;
        }
        for (Attribute attr : paramAttrs) {
            if (!first) writer.print(", ");
            writer.print(attr.getType() + " " + attr.getName());
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

        for (Attribute attr : paramAttrs) {
            writer.println("        this." + attr.getName() + " = " + attr.getName() + ";");
        }

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
        List<String> generatedFields = new ArrayList<>();

        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String fieldName = GeneratorUtils.makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (!generatedFields.contains(fieldName)) {
                    String type = getRelationshipType(assoc.getTargetClass(), assoc.getTargetMultiplicity(), assoc);
                    generateGetterSetter(writer, type, fieldName);
                    generatedFields.add(fieldName);
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String fieldName = GeneratorUtils.makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity());

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
            if (op.isAbstract()) {
                String absReturn = GeneratorUtils.removePackageNames(op.getReturnType());
                writer.print("    " + op.getVisibility() + " abstract " + absReturn + " " + op.getName() + "(");
                List<Parameter> absParams = op.getParameters();
                for (int i = 0; i < absParams.size(); i++) {
                    Parameter p = absParams.get(i);
                    if (i > 0) writer.print(", ");
                    writer.print(GeneratorUtils.removePackageNames(p.getType()) + " " + p.getName());
                }
                writer.println(");");
                continue;
            }
            Interaction interaction = op.getSequenceDiagram();
            if (interaction != null) {
                OperationGenerator.generateOperation(writer, cls, op, interaction, associations);
            } else {
                // fallback, se a operação nao tiver diagrama sequencia respetivo
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

    // gera um metodo toString para a classe
    private void generateToString(PrintWriter writer, Class cls) {
        writer.println("    @Override");
        writer.println("    public String toString() {");
        writer.print("        return \"" + cls.getName() + "{\" +");
        writer.println();
        writer.print("            \"id=\" + getId()");

        for (Attribute attr : cls.getAttributes()) {
            writer.println(" +");
            String capitalized = GeneratorUtils.capitalizeFirst(attr.getName());
            String expr;
            if (attr.hasDerive()) {
                expr = "get" + capitalized + "()";
            } else {
                expr = attr.getName();
            }
            if (attr.getType().equals("String")) {
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

    // gera com base se é map list ou normal
    private void generateRelationshipField(PrintWriter writer, String otherClass, String fieldName, String multiplicity, Association assoc) {
        if (assoc.hasQualifier()) {
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " + fieldName + " = new HashMap<>();");
        } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
            writer.println("    private List<" + otherClass + "> " + fieldName + " = new ArrayList<>();");
        } else {
            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }

    // se é map ou list
    private String getRelationshipType(String otherClass, String multiplicity, Association assoc) {
        if (assoc.hasQualifier()) {
            return "Map<" + assoc.getQualifierType() + ", " + otherClass + ">";
        } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
            return "List<" + otherClass + ">";
        } else {
            return otherClass;
        }
    }

}

