package generator.plain;

import generator.GeneratorUtils;
import generator.OperationGenerator;
import generator.ValidationGenerator;
import model.Class;
import model.Enumeration;
import model.*;
import model.sequence.Interaction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/*
* Gera uma classe java por cada UML Classe do modelo
* para cada classe classe:
*  - atributos base
*  - atributos de associação e inicilização no construtor
*  - getters e setters para atributos base
*  - métodos de associação (add/remove para multiplicidade *, set para 1)
*  - operações (com base em interações, se existirem)
*
*  - OCL
*  - invariants: metodo validate + mensagens de erro
*  - derived: getter com expressão java equivalente
*  - init: atribuição no construtor

* */

public class ClassGenerator {
    private String outputDirectory;
    private String basePackage;
    private ValidationGenerator validationGenerator;
    private List<Interaction> interactions;

    public ClassGenerator(String outputDirectory, String basePackage, List<Interaction> interactions) {
        this.outputDirectory = outputDirectory;
        this.basePackage = basePackage;
        this.interactions = interactions;
    }

    // diretoria onde os ficheiros são escritos: <output>/<package como caminho>
    private String packageDir() {
        String dir = outputDirectory + "/" + basePackage.replace('.', '/');
        new File(dir).mkdirs();
        return dir;
    }

    public void generateClasses(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        this.validationGenerator = new ValidationGenerator();
        // gerar as "classes enum"
        for (Enumeration enumeration : enumerations) {
            generateEnum(enumeration);
        }
        for (Class cls : classes) {
            generateClass(cls, associations);
        }
    }

    /**
     * gera as interfaces UML.
     */
    public void generateInterfaces(List<Interface> interfaces) throws IOException {
        if (interfaces == null || interfaces.isEmpty()) return;

        for (Interface iface : interfaces) {
            String filename = packageDir() + "/" + iface.getName() + ".java";
            try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
                w.println("package " + basePackage + ";");
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

    private void generateEnum(Enumeration enumeration) throws IOException {
        String filename = packageDir() + "/" + enumeration.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("package " + basePackage + ";");
            writer.println();
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
        String filename = packageDir() + "/" + cls.getName() + ".java";

        try(PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ";");
            writer.println();

            // imports
            writer.println("import java.util.*;");
            writer.println("import java.time.*;");   // para atributos de data (LocalDate, etc.)
            writer.println();

            // class declaration and if abstract
            writer.print("public " + (cls.isAbstract() ? "abstract " : "") + "class " + cls.getName());

            // if has parents
            if (cls.getParentClass() != null){
                writer.print(" extends " + cls.getParentClass().getName());
            }
            // if has interface
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
            OperationGenerator.generateOperations(writer, cls, associations);
            writer.println();

            // validation (OCL invariants)
            validationGenerator.generateValidateMethod(writer, cls);

            writer.println("}");
        }

        System.out.println("Generated class: " + filename);
    }

    /*
    * gera os atributos base da classe,
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
     * gera as associações (navegaveis) da classe
     * verifica para cada associação a classe target/source e multiplicaideda para decidir o atributo
     * ou o caso de ser um mapa
     */
    private void generateAssociations(PrintWriter writer, Class cls, List<Association> associations){
        // so para prevenir no caso de nao haver roles para nao duplicar atributos com o mesmo nome de classe
        List<String> generatedAttributes = new ArrayList<>();

        writer.println("    // Associations");

        for (Association association : associations){

            // class é source e nav
            if (association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()){
                String targetClass = association.getTargetClass();
                String multiplicity = association.getTargetMultiplicity();
                String roleName = association.getTargetRole();
                // attribute name com base na target class e multiplicity
                String attributeName = GeneratorUtils.makeAttributeName(targetClass, roleName, multiplicity);

                if (!generatedAttributes.contains(attributeName)){
                    // maps só na ponta que tem o qualifier
                    if (association.isMapEnd(true)){
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

            // class é target e nav
            if (association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()){
                String sourceClass = association.getSourceClass();
                String multiplicity = association.getSourceMultiplicity();
                String roleName = association.getSourceRole();
                String attributeName = GeneratorUtils.makeAttributeName(sourceClass, roleName, multiplicity);

                if (!generatedAttributes.contains(attributeName)){
                    // o Map é deste se o qualifier não está na source
                    if (association.isMapEnd(false)){
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

        writer.println("    public " + cls.getName() + "() {");

        if (cls.getParentClass() != null){
            writer.println("        super();");
        }

        for (Association association : associations) {
            //  class é source e  nav
            if (association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()) {
                String multiplicity = association.getTargetMultiplicity();
                String targetClass = association.getTargetClass();
                String roleName = association.getTargetRole();
                String attributeName = GeneratorUtils.makeAttributeName(targetClass, roleName, multiplicity);

                if (association.isMapEnd(true)) {
                    writer.println("        this." + attributeName + " = new HashMap<>();");
                } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
                    writer.println("        this." + attributeName + " = new ArrayList<>();");
                }
            }

            // class é  target e nav
            if (association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()) {
                String multiplicity = association.getSourceMultiplicity();
                String sourceClass = association.getSourceClass();
                String roleName = association.getSourceRole();
                String attributeName = GeneratorUtils.makeAttributeName(sourceClass, roleName, multiplicity);

                if (association.isMapEnd(false)) {
                    writer.println("        this." + attributeName + " = new HashMap<>();");
                } else if (GeneratorUtils.checkMultiplicity(multiplicity)) {
                    writer.println("        this." + attributeName + " = new ArrayList<>();");
                }
            }
        }

        // attribute inits do ocl
        for (Attribute attribute : cls.getAttributes()) {
            if (attribute.hasInit()) {
                writer.println("        this." + attribute.getName() + " = " + attribute.getJavaInitExpression() + ";");
            }
        }

        writer.println("    }");
    }

    /*
    * gera getters e setters para os atributos da classe
    * para atributos derivados, gera apenas o getter
     */
    private void generateGettersSetters(PrintWriter writer, Class cls){
        writer.print("    // Getters and Setters");

        for (Attribute attribute : cls.getAttributes()) {
            String name = attribute.getName();
            String type = attribute.getType();
            String capitalizedName = GeneratorUtils.capitalizeFirst(name);

            if (attribute.hasDerive()){
                writer.println();
                writer.println("    public " + type + " get" + capitalizedName + "() {");
                writer.println("        return " + attribute.getJavaDeriveExpression() + ";");
                writer.println("    }");
                continue; //nao tem setter
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
     * gera os metodos das associação (add/remove/get) com base na multiplicidade e qualifiers
     * Para multiplicidade *, gera add/remove/getAll
     * Para multiplicidade 1, gera get/set
     * Para qualifiers, gera put/get/remove por chave e getAll
     */
    private void generateAssociationMethods(PrintWriter writer, Class cls, List<Association> associations) {
        for (Association association: associations) {

            // class é a source
            if (association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()) {
                String targetClass = association.getTargetClass();
                String multiplicity = association.getTargetMultiplicity();
                String roleName = association.getTargetRole();
                String attributeName = GeneratorUtils.makeAttributeName(targetClass, roleName, multiplicity);

                // para maps só na ponta que tem o qualifier (aqui é a source)
                if (association.isMapEnd(true)){
                    String keyType = association.getQualifierType();
                    generateMapAssociationMethods(writer, targetClass, attributeName, keyType);
                }
                // lists
                else if(GeneratorUtils.checkMultiplicity(multiplicity)) {
                    generateListAssociationMethods(writer, targetClass, attributeName);
                }
                // single associations
                else {
                    generateSingleAssociationMethods(writer, targetClass, attributeName);
                }
            }

            // class é o target
            if (association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()) {
                String sourceClass = association.getSourceClass();
                String multiplicity = association.getSourceMultiplicity();
                String roleName = association.getSourceRole();
                String attributeName = GeneratorUtils.makeAttributeName(sourceClass, roleName, multiplicity);

                if (association.isMapEnd(false)){
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
        // nome dos métodos baseado no role (attributeName)
        String cap = GeneratorUtils.capitalizeFirst(attributeName);
        // put method
        writer.println();
        writer.println("    public void put" + cap + "(" + keyType + " key, " + targetClass + " value) {");
        writer.println("        this." + attributeName + ".put(key, value);");
        writer.println("    }");

        // get method
        writer.println();
        writer.println("    public " + targetClass + " get" + cap + "(" + keyType + " key) {");
        writer.println("        return this." + attributeName + ".get(key);");
        writer.println("    }");

        // remove method
        writer.println();
        writer.println("    public void remove" + cap + "(" + keyType + " key) {");
        writer.println("        this." + attributeName + ".remove(key);");
        writer.println("    }");

        // get all method
        writer.println();
        writer.println("    public Map<" + keyType + ", " + targetClass + "> getAll" + cap + "() {");
        writer.println("        return this." + attributeName + ";");
        writer.println("    }");
    }

    /*
    *  métodos de associação para associações com multiplicidade * (listas)
    *  add / remove / getXs
     */
    private void generateListAssociationMethods(PrintWriter wrriter, String className, String attributeName){
        String cap = GeneratorUtils.capitalizeFirst(attributeName);   // nome dos metodos baseado na role
        // add method
        wrriter.println();
        wrriter.println("    public void add" + cap + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + ".add(" + attributeName + ");");
        wrriter.println("    }");

        // remove method
        wrriter.println();
        wrriter.println("    public void remove" + cap + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + ".remove(" + attributeName + ");");
        wrriter.println("    }");

        // get method
        wrriter.println();
        wrriter.println("    public List<" + className + "> get" + cap + "() {");
        wrriter.println("        return this." + attributeName + ";");
        wrriter.println("    }");
    }

    /*
    *  métodos de associação para associações com multiplicidade 1 (atributo simples)
    * get / set
     */
    private void generateSingleAssociationMethods(PrintWriter wrriter, String className, String attributeName){
        String cap = GeneratorUtils.capitalizeFirst(attributeName);   // nome dos metodos baseado na role
        // get method
        wrriter.println();
        wrriter.println("    public " + className + " get" + cap + "() {");
        wrriter.println("        return this." + attributeName + ";");
        wrriter.println("    }");

        // set method
        wrriter.println();
        wrriter.println("    public void set" + cap + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + " = " + attributeName + ";");
        wrriter.println("    }");
    }
}
