package generator.plain;

import generator.GeneratorUtils;
import generator.OperationGenerator;
import generator.ValidationGenerator;
import model.Class;
import model.Enumeration;
import model.*;
import model.sequence.Interaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/*
* Gera uma class java por UML Classe do modelo
* Por classe, em ordem:
*  - atributos base
*  - atributos de associação e inicilização no construtor
*  - getters e setters para atributos base
*  - métodos de associação (add/remove para multiplicidade *, set para 1)
*  - operações (com base em interações, se existirem)
*
*  - OCL Part
*  - invariants: metodo validate + mensagens de erro
*  - derived: getter com expressão java equivalente
*  - init: atribuição no construtor

* */

public class ClassGenerator {
    private String outputDirectory;
    private ValidationGenerator validationGenerator;
    private List<Interaction> interactions;

    public ClassGenerator(String outputDirectory, List<Interaction> interactions) {
        this.outputDirectory = outputDirectory;
        this.interactions = interactions;
    }

    public void generateClasses(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        this.validationGenerator = new ValidationGenerator();
        // Gerar as "classes enum"
        for (Enumeration enumeration : enumerations) {
            generateEnum(enumeration);
        }
        for (Class cls : classes) {
            generateClass(cls, associations);
        }
    }

    /**
     * Gera um ficheiro Java por interface UML (só assinaturas).
     */
    public void generateInterfaces(List<Interface> interfaces) throws IOException {
        if (interfaces == null || interfaces.isEmpty()) return;

        for (Interface iface : interfaces) {
            String filename = outputDirectory + "/" + iface.getName() + ".java";
            try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
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

    private void generateEnum(Enumeration enumeration) throws IOException {
        String filename = outputDirectory + "/" + enumeration.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("public enum " + enumeration.getName() + " {");

            List<String> literals = enumeration.getLiterals();
            for (int i = 0; i < literals.size(); i++) {
                String separator = (i < literals.size() - 1) ? "," : ";";
                writer.println("    " + literals.get(i) + separator);
            }

            writer.println("}");
        }

        System.out.println("Generated enum: " + filename);
    }

    private void generateClass(Class cls, List<Association> associations) throws IOException {
        String filename = outputDirectory + "/" + cls.getName() + ".java";

        try(PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // imports
            writer.println("import java.util.*;");
            writer.println();

            // class
            writer.print("public " + (cls.isAbstract() ? "abstract " : "") + "class " + cls.getName());

            if (cls.getParentClass() != null){
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

            // attributes
            generateAttributes(writer, cls);
            writer.println();

            // associations
            generateAssociations(writer, cls, associations);
            writer.println();

            // constructor
            generateConstructors(writer, cls, associations);
            writer.println();

            // getters and setters
            generateGettersSetters(writer, cls);
            writer.println();

            // association methods
            generateAssociationMethods(writer, cls, associations);
            writer.println();

            // generate operations
            generateOperations(writer, cls, associations);
            writer.println();

            // validation (OCL invariants)
            if (!cls.getInvariants().isEmpty()) {
                validationGenerator.generateValidateMethod(writer, cls);
            }

            writer.println("}");
        }

        System.out.println("Generated class: " + filename);
    }

    /*
    * Gera os atributos base da classe,
    * ignora os derivados, sao criados depois
     */
    private void generateAttributes(PrintWriter writer, Class cls){
        writer.println("    // Attributes");

        for (Attribute attribute : cls.getAttributes()){
            if (attribute.hasDerive()) continue;
            writer.println("    " + attribute.getVisibility() + " " + attribute.getType() + " " + attribute.getName() + ";");
        }
    }

    /**
     * Gera as associações (navegaveis) da classe
     * Verifica para cada associação a classe target/source e multiplicaideda para decidir o atributo
     * Verifica tambem por qualifiers (maps)
     */
    private void generateAssociations(PrintWriter writer, Class cls, List<Association> associations){
        List<String> generatedAttributes = new ArrayList<>();

        writer.println("    // Associations");

        for (Association association : associations){

            // where class is source and navigable
            if (association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()){
                String targetClass = association.getTargetClass();
                String multiplicity = association.getTargetMultiplicity();
                String roleName = association.getTargetRole();
                // get the attribute name based on the target class and multiplicity
                String attributeName = GeneratorUtils.makeAttributeName(targetClass, roleName, multiplicity);

                if (!generatedAttributes.contains(attributeName)){
                    if (association.hasQualifier() ){
                        String keyType = association.getQualifierType();
                        writer.println("   " + "private Map<" + keyType + ", " + targetClass + "> " + attributeName + ";");
                    }
                    else if (GeneratorUtils.checkMultiplicity(multiplicity)){
                        writer.println("    " + "private List<" + targetClass + "> " + attributeName + ";");
                    } else {
                        writer.println("    " + "private " + targetClass + " " + attributeName + ";");
                    }
                    generatedAttributes.add(attributeName);
                }
            }

            // where class is target and navigable
            if (association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()){
                String sourceClass = association.getSourceClass();
                String multiplicity = association.getSourceMultiplicity();
                String roleName = association.getSourceRole();
                String attributeName = GeneratorUtils.makeAttributeName(sourceClass, roleName, multiplicity);

                if (!generatedAttributes.contains(attributeName)){
                    if (association.hasQualifier() ){
                        String keyType = association.getQualifierType();
                        writer.println("   " + "private Map<" + keyType + ", " + sourceClass + "> " + attributeName + ";");
                    }
                    else if (GeneratorUtils.checkMultiplicity(multiplicity)){
                        writer.println("    " + "private List<" + sourceClass + "> " + attributeName + ";");
                    } else {
                        writer.println("    " + "private " + sourceClass + " " + attributeName + ";");
                    }
                    generatedAttributes.add(attributeName);
                }
            }
        }
    }

    /**
     * Construtur base apenas, sem parametros, mas inicializa associaçºoes
     */
    private void generateConstructors(PrintWriter writer, Class cls, List<Association> associations){
        writer.println("    // Constructor");
        // only the default for now at least
        writer.println("    public " + cls.getName() + "() {");

        if (cls.getParentClass() != null){
            writer.println("        super();");
        }

        for (Association association : associations) {
            // where class is source and nav
            if (association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()) {
                String multiplicity = association.getTargetMultiplicity();
                String targetClass = association.getTargetClass();
                String roleName = association.getTargetRole();
                String attributeName = GeneratorUtils.makeAttributeName(targetClass, roleName, multiplicity);

                if (association.hasQualifier()) {
                    writer.println("        this." + attributeName + " = new HashMap<>();");
                } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
                    writer.println("        this." + attributeName + " = new ArrayList<>();");
                }
            }

            // where class is target and nav
            if (association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()) {
                String multiplicity = association.getSourceMultiplicity();
                String sourceClass = association.getSourceClass();
                String roleName = association.getSourceRole();
                String attributeName = GeneratorUtils.makeAttributeName(sourceClass, roleName, multiplicity);

                if (association.hasQualifier()) {
                    writer.println("        this." + attributeName + " = new HashMap<>();");
                } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
                    writer.println("        this." + attributeName + " = new ArrayList<>();");
                }
            }
        }

        // attribute inits (OCL)
        for (Attribute attribute : cls.getAttributes()) {
            if (attribute.hasDerive()) continue;
            if (attribute.hasInit()) {
                writer.println("        this." + attribute.getName() + " = " + attribute.getJavaInitExpression() + ";");
            }
        }

        writer.println("    }");
    }

    /*
    * Gera getters e setters para os atributos da classe
    * Para atributos derivados, gera apenas o getter com a expressão java equivalente
     */
    private void generateGettersSetters(PrintWriter writer, Class cls){
        writer.print("    // Getters and Setters");

        for (Attribute attribute : cls.getAttributes()) {
            String name = attribute.getName();
            String type = attribute.getType();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);

            if (attribute.hasDerive()){
                writer.println();
                writer.println("    public " + type + " get" + capitalizedName + "() {");
                writer.println("        return " + attribute.getJavaDeriveExpression() + ";");
                writer.println("    }");
                continue; // no setter for derived attributes
            }

            // get
            writer.println();
            writer.println("    public " + type + " get" + capitalizedName + "() {");
            writer.println("        return " + name + ";");
            writer.println("    }");

            // set
            writer.println();
            writer.println("    public void set" + capitalizedName + "(" + type + " " + name + ") {");
            writer.println("        this." + name + " = " + name + ";");
            writer.println("    }");
        }
    }

    /**
     *
     * Gera os métodos de associação (add/remove/get) com base na multiplicidade e qualifiers
     * Para multiplicidade *, gera add/remove/getAll
     * Para multiplicidade 1, gera get/set
     * Para qualifiers, gera put/get/remove por chave e getAll
     */
    private void generateAssociationMethods(PrintWriter writer, Class cls, List<Association> associations) {
        for (Association association: associations) {

            // where class is the source
            if (association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()) {
                String targetClass = association.getTargetClass();
                String multiplicity = association.getTargetMultiplicity();
                String roleName = association.getTargetRole();
                String attributeName = GeneratorUtils.makeAttributeName(targetClass, roleName, multiplicity);

                if (association.hasQualifier()){
                    String keyType = association.getQualifierType();
                    generateMapAssociationMethods(writer, targetClass, attributeName, keyType);
                }
                else if(GeneratorUtils.checkMultiplicity(multiplicity)) {
                    generateListAssociationMethods(writer, targetClass, attributeName);
                } else {
                    generateSingleAssociationMethods(writer, targetClass, attributeName);
                }
            }

            if (association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()) {
                String sourceClass = association.getSourceClass();
                String multiplicity = association.getSourceMultiplicity();
                String roleName = association.getSourceRole();
                String attributeName = GeneratorUtils.makeAttributeName(sourceClass, roleName, multiplicity);

                if (association.hasQualifier()){
                    String keyType = association.getQualifierType();
                    generateMapAssociationMethods(writer, sourceClass, attributeName, keyType);
                }
                else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
                    generateListAssociationMethods(writer, sourceClass, attributeName);
                } else {
                    generateSingleAssociationMethods(writer, sourceClass, attributeName);
                }
            }
        }
    }

    /*
    *  métodos de associação para associações com qualifiers (maps)
    * put / get / remove / getAll
     */
    private void generateMapAssociationMethods(PrintWriter writer, String targetClass, String attributeName, String keyType){
        // put method
        writer.println();
        writer.println("    public void put" + targetClass + "(" + keyType + " key, " + targetClass + " value) {");
        writer.println("        this." + attributeName + ".put(key, value);");
        writer.println("    }");

        // get method
        writer.println();
        writer.println("    public " + targetClass + " get" + targetClass + "(" + keyType + " key) {");
        writer.println("        return this." + attributeName + ".get(key);");
        writer.println("    }");

        // remove method
        writer.println();
        writer.println("    public void remove" + targetClass + "(" + keyType + " key) {");
        writer.println("        this." + attributeName + ".remove(key);");
        writer.println("    }");

        // get all method
        writer.println();
        writer.println("    public Map<" + keyType + ", " + targetClass + "> getAll" + targetClass + "s() {");
        writer.println("        return this." + attributeName + ";");
        writer.println("    }");
    }

    /*
    *  métodos de associação para associações com multiplicidade * (listas)
    *  add / remove / getXs
     */
    private void generateListAssociationMethods(PrintWriter wrriter, String className, String attributeName){
        // add method
        wrriter.println();
        wrriter.println("    public void add" + className + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + ".add(" + attributeName + ");");
        wrriter.println("    }");

        // remove method
        wrriter.println();
        wrriter.println("    public void remove" + className + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + ".remove(" + attributeName + ");");
        wrriter.println("    }");

        // get method
        wrriter.println();
        wrriter.println("    public List<" + className + "> get" + className + "s() {");
        wrriter.println("        return this." + attributeName + ";");
        wrriter.println("    }");
    }

    /*
    *  métodos de associação para associações com multiplicidade 1 (atributo simples)
    * get / set
     */
    private void generateSingleAssociationMethods(PrintWriter wrriter, String className, String attributeName){
        // get method
        wrriter.println();
        wrriter.println("    public " + className + " get" + className + "() {");
        wrriter.println("        return this." + attributeName + ";");
        wrriter.println("    }");

        // set method
        wrriter.println();
        wrriter.println("    public void set" + className + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + " = " + attributeName + ";");
        wrriter.println("    }");
    }

    /**
     *
     * Gera os métodos das operações da classe, com base nas interações do modelo
     */
    private void generateOperations(PrintWriter writer, Class cls, List<Association> associations){
        if (cls.getOperations().isEmpty()){
            return;
        }

        writer.println("    // Operations");

        for (Operation op : cls.getOperations()) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction != null) {
                OperationGenerator.generateOperation(writer, cls, op, interaction, associations);
            } else {
                // fallback: assinatura + stub
                String visibility = op.getVisibility();
                String returnType = GeneratorUtils.removePackageNames(op.getReturnType());
                String name = op.getName();
                writer.println("    " + visibility + " " + returnType + " " + name + "(");

                List<Parameter> parameters = op.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    Parameter param = parameters.get(i);
                    String paramType = GeneratorUtils.removePackageNames(param.getType());
                    String paramName = param.getName();
                    writer.print("        " + paramType + " " + paramName);
                    writer.println(i < parameters.size() - 1 ? "," : "");
                }

                writer.println("    ) {");
                writer.println("        throw new UnsupportedOperationException(\"Not implemented yet\");");
                writer.println("    }");
            }
        }
    }

}
