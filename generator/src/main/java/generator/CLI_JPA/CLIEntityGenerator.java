package generator.CLI_JPA;

import generator.JpaEntityWriter;
import generator.GeneratorUtils;
import generator.RelationshipInfo;
import generator.OperationGenerator;
import generator.ValidationGenerator;
import model.*;
import model.Class;
import model.sequence.Interaction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gera as entidades JPA (CLI-JPA): @Entity, id, atributos, relações, getters/setters, toString, validate, operações.
 */
public class CLIEntityGenerator {
    private String baseOutputPath;
    private String basePackage;

    private ValidationGenerator validationGenerator;

    // nomes das enumerações para saber quando um atributo é enum
    private Set<String> enumNames = new HashSet<>();

    public CLIEntityGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    public void generateEntities(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating CLIJPA entities...");

        // criar path domain
        String domainPath = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/models";
        new File(domainPath).mkdirs();

        this.validationGenerator = new ValidationGenerator();

        // guardar nomes das enumerações
        enumNames.clear();
        for (Enumeration enumeration : enumerations) {
            enumNames.add(enumeration.getName());
        }

        // gerar enums
        for (Enumeration enumeration : enumerations) {
            generateEnum(enumeration, domainPath);
        }

        for (Class cls : classes) {
            generateEntity(cls, classes, associations, domainPath);
        }

        System.out.println("Entity generation completed.");
    }

    // gera os enums como enums Java normais, com @Enumerated nos atributos que os usam
    private void generateEnum(Enumeration enumeration, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + enumeration.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println("package " + basePackage + ".models;");
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

    /**
     * gerar uma entidade
     * */
    private void generateEntity(Class cls, List<Class> allClasses, List<Association> associations, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + cls.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(filename)) {
            // package
            writer.println("package " + basePackage + ".models;");
            writer.println();

            // imports
            writer.println("import jakarta.persistence.*;");
            writer.println("import java.util.*;");
            if (GeneratorUtils.needsJavaTime(cls)) writer.println("import java.time.*;");   // atributos de data (LocalDate, etc.)
            if (GeneratorUtils.needsJavaMath(cls)) writer.println("import java.math.*;");   // atributos BigDecimal/BigInteger
            writer.println();

            // anotações base da classe
            generateClassBaseAnnotations(writer, cls, allClasses);

            // declaração da classe
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

            // se nao tem parent gera id
            if (cls.getParentClass() == null) {
                JpaEntityWriter.generateId(writer, cls);
            }

            // atributos
            generateAttributes(writer, cls);

            // getters derivados
            JpaEntityWriter.generateDerivedGetters(writer, cls);

            // relações
            generateRelationships(writer, cls, associations, allClasses);

            generateBaseStuff(writer, cls, associations);

            validationGenerator.generateValidateMethod(writer, cls);

            OperationGenerator.generateOperations(writer, cls, associations);

            writer.println("}");

            System.out.println("Generated: " + cls.getName() + ".java");
        }
    }

    /**
     *  anotações base da classe
     */
    private void generateClassBaseAnnotations(PrintWriter writer, Class cls, List<Class> allClasses) {
        // estratégia de herança
        String strategy = cls.getRootInheritanceStrategy();

        writer.println("@Entity");

        if (strategy.equals("SINGLE_TABLE") && cls.getParentClass() != null) {
            // para herança single table, só a classe pai tem @Table

            // filho em SINGLE_TABLE partilha a tabela do pai, por isso não leva @Table.
            // so fica com @DiscriminatorValue
            return;
        } else {
            // nome derivado ou override do painel Persistence
            writer.println("@Table(name = \"" + GeneratorUtils.tableName(cls) + "\")");
        }

        // caso seja parent, gerar @Inheritance
        if (cls.getParentClass() == null && GeneratorUtils.isParent(cls, allClasses)) {
            // pai
            if (strategy.equals("SINGLE_TABLE")) {
                writer.println("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)");
                writer.println("@DiscriminatorColumn(name = \"dtype\")");
                writer.println("@DiscriminatorValue(\"" + cls.getName() + "\")");
            } else if (strategy.equals("JOINED")) {
                writer.println("@Inheritance(strategy = InheritanceType.JOINED)");
            }
        } else if (cls.getParentClass() != null) {
            // filho
            if (strategy.equals("SINGLE_TABLE")) {
                writer.println("@DiscriminatorValue(\"" + cls.getName() + "\")");
            } else if (strategy.equals("JOINED")) {
                writer.println("@PrimaryKeyJoinColumn(name = \"" + cls.getParentClass().getName().toLowerCase() + "_id\")");
            }
        }
    }

    private void generateBaseStuff(PrintWriter writer, Class cls, List<Association> associations) {
        String className = cls.getName();

        // no arg constructor (preciso pelo JPA)
        writer.println();
        writer.println("    public " + className + "() {}");

        // getters/setters dos atributos
        // o @Id usa Integer/Long (não int/long)  senão getX() == null dava erro
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasDerive()) continue;
            String name = attr.getName();
            String type = name.equals(cls.getIdAttributeName())
                    ? cls.getRoot().getIdAttributeType()
                    : attr.getAttributeType();
            String cap  = capitalize(name);
            writer.println();
            writer.println("    public " + type + " get" + cap + "() { return " + name + "; }");
            writer.println("    public void set" + cap + "(" + type + " " + name + ") { this." + name + " = " + name + "; }");
        }

        // getid e set id so na raiz, se o id for custom o getter/setter ja vem do loop dos atributos
        if (cls.getParentClass() == null && cls.getIdAttributeName() == null) {
            String idType = cls.getRoot().getIdAttributeType();
            writer.println();
            writer.println("    public " + idType + " getId() { return id; }");
            writer.println("    public void setId(" + idType + " id) { this.id = id; }");
        }

        // getters and setters for relationship fields
        List<String> generatedRelations = new ArrayList<>();
        for (RelationshipInfo rel : RelationshipInfo.collectOwn(cls, associations)) {
            generateRelationshipGetterSetter(writer, rel.otherClass, rel.role, rel.thisMul(), rel.otherMultiplicity, rel.assoc, rel.isSource, generatedRelations);
        }


        // o toString não inclui as relações para evita loops infinitis
        writer.println();
        writer.println("    @Override");
        writer.println("    public String toString() {");
        StringBuilder sb = new StringBuilder();
        sb.append("        return \"").append(className).append("{\"");
        boolean first = true;
        if (cls.getRoot().getIdAttributeName() == null) {
            sb.append(" + \"id=\" + getId()");
            first = false;
        }
        for (Attribute attr : cls.getAllAttributes()) {
            if (attr.hasDerive()) continue;
            String sep = first ? "" : ", ";
            sb.append(" + \"").append(sep).append(attr.getName()).append("=\" + get").append(capitalize(attr.getName())).append("()");
            first = false;
        }
        sb.append(" + \"}\";");
        writer.println(sb.toString());
        writer.println("    }");
    }

    // isSource -> a classe que estamos a gerar está na ponta source da assoc ou nao
    private void generateRelationshipGetterSetter(PrintWriter writer,
                                          String otherClass,
                                          String otherRole,
                                          String thisMult,
                                          String otherMult,
                                          Association assoc,
                                          boolean isSource,
                                          List<String> generated) {
        // nome do atributo a gerar
        String attributeName = GeneratorUtils.makeAttributeName(otherClass, otherRole, otherMult);
        if (generated.contains(attributeName)) return;
        generated.add(attributeName);

        String type = otherClass;

        if (assoc.isMapEnd(isSource)) {
            type = "Map<" + assoc.getQualifierType() + ", " + otherClass + ">";
        }
        else if (GeneratorUtils.checkMultiplicity(otherMult)) {   // multi side
            type = "List<" + otherClass + ">";
        }
        String cap  = capitalize(attributeName);

        writer.println();
        writer.println("    public " + type + " get" + cap + "() { return " + attributeName + "; }");
        writer.println("    public void set" + cap + "(" + type + " " + attributeName + ") { this." + attributeName +
                " = " + attributeName + "; }");
    }

    private void generateAttributes(PrintWriter writer, Class cls) {
        writer.println("    // attributes");
        for (Attribute atr : cls.getAttributes()){

            if (atr.hasDerive()) continue;

            // colecao (List<String> Set<Long>) com @ElementCollection e init,
            if (atr.isCollection()) {
                writer.println("    @ElementCollection");
                String init = atr.getCollectionType().equals("List") ? "new ArrayList<>()" : "new HashSet<>()";
                writer.println("    private " + atr.getAttributeType() + " " + atr.getName() + " = " + init + ";");
                writer.println();
                continue;
            }

            // atributo escolhido como @Id só é definido na raiz.
            // o @Id usa Integer/Long (não int/long) senão getX() == null da erro
            boolean isId = atr.getName().equals(cls.getIdAttributeName());
            String attributeType = isId ? cls.getRoot().getIdAttributeType() : atr.getType();
            if (isId) {
                writer.println("    @Id");
                // chave se for string é atribuída pelo cliente -> sem @GeneratedValue
                if (!cls.isIdAssigned()) {
                    writer.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
                }
            }

            if (enumNames.contains(atr.getType())) {
                writer.println("    @Enumerated(EnumType.STRING)");
            }
            writer.println("    " + GeneratorUtils.columnAnnotation(atr));
            if (atr.hasInit()) {
                String javaInit = atr.getJavaInitExpression();
                writer.println("    private " + attributeType + " " + atr.getName() + " = " + javaInit + ";");
            } else {
                writer.println("    private " + attributeType + " " + atr.getName() + ";");
            }
            writer.println();
        }
    }

    /**
     * Intera as associações navegáveis e emite o campo de relação.
     */
    private void generateRelationships(PrintWriter writer, Class cls, List<Association> associations, List<Class> allClasses) {
        List<String> generatedRelations = new ArrayList<>();

        for (RelationshipInfo rel : RelationshipInfo.collectOwn(cls, associations)) {
            String attributeName = rel.attributeName();
            if (!generatedRelations.contains(attributeName)) {
                generateRelationshipBaseAnnotations(writer, cls.getName(), rel.otherClass, attributeName,
                        rel.otherMultiplicity, rel.assoc, rel.isSource, allClasses);
                generatedRelations.add(attributeName);
            }
        }
    }


    /**
     * gera as anotações de relacionamento (@OneToMany, @ManyToOne, @ManyToMany)
     * com base na multiplicidade e navegabilidade da associação.
     */
    private void generateRelationshipBaseAnnotations(PrintWriter writer, String thisClass,
                                                     String otherClass, String attributeName,
                                                     String multiplicity, Association assoc,
                                                     boolean isSource, List<Class> allClasses){
        writer.println("    // relationship: " + thisClass + " -> " + otherClass);

        // thisMultiplicity = multiplicidade do atributo desta classe
        // otherMultiplicity = quantos desta classe é que o outro lado vê
        String thisMultiplicity  = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
        String otherMultiplicity = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();

        // ponta com o Map conta como coleção
        boolean isCollection = GeneratorUtils.checkMultiplicity(thisMultiplicity) || assoc.isMapEnd(isSource);          // o atributo deste lado é coleção
        boolean inverseIsCollection = GeneratorUtils.checkMultiplicity(otherMultiplicity) || assoc.isMapEnd(!isSource); // o do outro lado
        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

        Class self = allClasses.stream().filter(c -> c.getName().equals(thisClass)).findFirst().orElseThrow();

        // @OrderBy da coleção
        if (isCollection) {
            String orderBy = self.getCollectionOrder().get(attributeName);
            if (orderBy != null) writer.println("    @OrderBy(\"" + orderBy + "\")");
        }

        // fetch das relações SINGULARES (@ManyToOne/@OneToOne); ausente = LAZY
        String fetch = self.getFetchStrategy().getOrDefault(attributeName, "LAZY");

        // Many to Many
        if (isCollection && inverseIsCollection) {
            JpaEntityWriter.generateManyToMany(writer, thisClass, otherClass, attributeName, assoc, isSource, isBidirectional);
        }
        // One to many
        else if (isCollection) {
            JpaEntityWriter.generateOneToMany(writer, thisClass, otherClass, attributeName, assoc, isSource, isBidirectional);
        }
        // Many to one
        else if (inverseIsCollection) {
            JpaEntityWriter.generateManyToOne(writer, otherClass, attributeName, assoc, fetch);
        }
        // One to one
        else {
            JpaEntityWriter.generateOneToOne(writer, thisClass, otherClass, attributeName, assoc, isSource, isBidirectional, fetch);
        }

        writer.println();
    }

    /**
     * gera os ficheiros para interface UML
     *
     */
    public void generateInterfaces(List<Interface> interfaces) throws IOException {
        if (interfaces == null || interfaces.isEmpty()) return;

        String dir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/models";
        new File(dir).mkdirs();

        for (Interface iface : interfaces) {
            try (PrintWriter w = new PrintWriter(dir + "/" + iface.getName() + ".java")) {
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


    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

}
