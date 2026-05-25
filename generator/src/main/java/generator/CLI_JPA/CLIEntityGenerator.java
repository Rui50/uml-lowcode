package generator.CLI_JPA;

import generator.GeneratorUtils;
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
 * Gera as entidades JPA
 * - por classe: anotações base de entity table etc, id
 * - atributos: tipo, coluna, enumeração, init
 * - relações: @OneToMany, @ManyToOne, @ManyToMany,
 * - getters derivados
 * - validate() se invs
 *
 * - Herança: por agora só SINGLE_TABLE, depois podemos configurar para JOINED
 */
public class CLIEntityGenerator {
    private String baseOutputPath;
    private String basePackage;

    // para o tipo de herança, para já user SINGLE_TABLE, depois podemos configurar
    private String inheritanceStrategy = "SINGLE_TABLE";

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
                generateId(writer);
            }

            // atributos
            generateAttributes(writer, cls);

            // getters derivados
            generateDerivedGetters(writer, cls);

            // relações
            generateRelationships(writer, cls, associations, allClasses);

            generateBaseStuff(writer, cls, associations);

            validationGenerator.generateValidateMethod(writer, cls);

            generateOperations(writer, cls, associations);

            writer.println("}");

            System.out.println("Generated: " + cls.getName() + ".java");
        }
    }

    /**
     *  anotações base
     */
    private void generateClassBaseAnnotations(PrintWriter writer, Class cls, List<Class> allClasses) {
        // @Entity
        writer.println("@Entity");

        if (inheritanceStrategy.equals("SINGLE_TABLE") && cls.getParentClass() != null) {
            // para herança single table, só a classe pai tem @Table
            // os têm @DiscriminatorValue

            // filho em SINGLE_TABLE: partilha a tabela do pai, por isso não leva @Table.
            // só recebe @DiscriminatorValue, gerado mais abaixo.
            return;
        } else {
            // @Table , para ja normalizar so para minusculas, depois pensar melhor
            // ter cuidado com nomes de classes que podem colidir depois de normalizar como (USER)
            writer.println("@Table(name = \"" + cls.getName().toLowerCase() + "\")");
        }

        // caso seja parent, gerar @Inheritance
        if (cls.getParentClass() == null && isParent(cls, allClasses)) {
            // pai
            if (inheritanceStrategy.equals("SINGLE_TABLE")) {
                writer.println("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)");
                writer.println("@DiscriminatorColumn(name = \"type\")");
                writer.println("@DiscriminatorValue(\"" + cls.getName() + "\")");
            } else if (inheritanceStrategy.equals("JOINED")) {
                writer.println("@Inheritance(strategy = InheritanceType.JOINED)");
            }
        } else if (cls.getParentClass() != null) {
            // filho
            if (inheritanceStrategy.equals("SINGLE_TABLE")) {
                writer.println("@DiscriminatorValue(\"" + cls.getName() + "\")");
            } else if (inheritanceStrategy.equals("JOINED")) {
                writer.println("@PrimaryKeyJoinColumn(name = \"" + cls.getParentClass().getName().toLowerCase() + "_id\")");
            }
        }
    }

    // para ja id auto gerado como long
    private void generateId(PrintWriter writer) {
        writer.println("    @Id");
        writer.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
        writer.println("    private Long id;");
        writer.println();
    }

    private void generateBaseStuff(PrintWriter writer, Class cls, List<Association> associations) {
        String className = cls.getName();

        // no-arg constructor (preciso pelo JPA)
        writer.println();
        writer.println("    public " + className + "() {}");

        // getters and setters for non-derived attributes
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasDerive()) continue;     // derived has its own getter already
            String type = attr.getType();
            String name = attr.getName();
            String cap  = capitalize(name);
            writer.println();
            writer.println("    public " + type + " get" + cap + "() { return " + name + "; }");
            writer.println("    public void set" + cap + "(" + type + " " + name + ") { this." + name + " = " + name + "; }");
        }

        // id getter/setter only on root class
        if (cls.getParentClass() == null) {
            writer.println();
            writer.println("    public Long getId() { return id; }");
            writer.println("    public void setId(Long id) { this.id = id; }");
        }

        // getters and setters for relationship fields
        List<String> emittedRelations = new ArrayList<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String otherClass = assoc.getTargetClass();
                String otherRole = assoc.getTargetRole();
                String thisMult = assoc.getSourceMultiplicity();
                String otherMult = assoc.getTargetMultiplicity();
                generateRelationshipGetterSetter(writer, otherClass, otherRole, thisMult, otherMult, assoc, emittedRelations);
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String otherClass = assoc.getSourceClass();
                String otherRole = assoc.getSourceRole();
                String thisMult = assoc.getTargetMultiplicity();
                String otherMult = assoc.getSourceMultiplicity();
                generateRelationshipGetterSetter(writer, otherClass, otherRole, thisMult, otherMult, assoc, emittedRelations);
            }
        }


        // toString doesnt include relationships for now, avoids infinite cycle
        writer.println();
        writer.println("    @Override");
        writer.println("    public String toString() {");
        StringBuilder sb = new StringBuilder();
        sb.append("        return \"").append(className).append("{id=\" + getId()");
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasDerive()) continue;
            sb.append(" + \", ").append(attr.getName()).append("=\" + ").append(attr.getName());
        }
        sb.append(" + \"}\";");
        writer.println(sb.toString());
        writer.println("    }");
    }

    private void generateRelationshipGetterSetter(PrintWriter writer,
                                          String otherClass,
                                          String otherRole,
                                          String thisMult,
                                          String otherMult,
                                          Association assoc,
                                          List<String> emitted) {
        String fieldName = GeneratorUtils.makeAttributeName(otherClass, otherRole, otherMult);
        if (emitted.contains(fieldName)) return;
        emitted.add(fieldName);

        String type = otherClass;
        if (assoc.hasQualifier()) {
            type = "Map<" + assoc.getQualifierType() + ", " + otherClass + ">";
        }
        else if (GeneratorUtils.checkMultiplicity(otherMult)) {   // multi side
            type = "List<" + otherClass + ">";
        }
        String cap  = capitalize(fieldName);

        writer.println();
        writer.println("    public " + type + " get" + cap + "() { return " + fieldName + "; }");
        writer.println("    public void set" + cap + "(" + type + " " + fieldName + ") { this." + fieldName +
                " = " + fieldName + "; }");
    }

    private void generateAttributes(PrintWriter writer, Class cls) {
        writer.println("    // attributes");
        for (Attribute atr : cls.getAttributes()){
            // skip derived
            if (atr.hasDerive()) continue;

            if (enumNames.contains(atr.getType())) {
                writer.println("    @Enumerated(EnumType.STRING)");
            }
            writer.println("    @Column(name = \"" + atr.getName() + "\")");
            if (atr.hasInit()) {
                String javaInit = atr.getJavaInitExpression();
                writer.println("    private " + atr.getType() + " " + atr.getName() + " = " + javaInit + ";");
            } else {
                writer.println("    private " + atr.getType() + " " + atr.getName() + ";");
            }
            writer.println();
        }
    }

    private void generateDerivedGetters(PrintWriter writer, Class cls) {
        for (Attribute atr : cls.getAttributes()) {
            if (!atr.hasDerive()) continue;
            String cap = atr.getName().substring(0,1).toUpperCase() + atr.getName().substring(1);
            writer.println("    public " + atr.getType() + " get" + cap + "() {");
            writer.println("        return " + atr.getJavaDeriveExpression() + ";");
            writer.println("    }");
            writer.println();
        }
    }

    /**
     * Intera as associações navegáveis e emite o campo de relação.
     */
    private void generateRelationships(PrintWriter writer, Class cls, List<Association> associations, List<Class> allClasses) {
        List<String> generatedRelations = new ArrayList<>();

        for(Association association : associations) {
            if(association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()){
                String targetClass = association.getTargetClass();
                String targetRole = association.getTargetRole();
                String targetMultiplicity = association.getTargetMultiplicity();

                String fieldName = GeneratorUtils.makeAttributeName(targetClass, targetRole, targetMultiplicity);

                if(!generatedRelations.contains(fieldName)){
                    generateRelationshipBaseAnnotations(writer, cls.getName(), targetClass, fieldName, targetMultiplicity, association, "target", allClasses);
                    generatedRelations.add(fieldName);
                }
            }

            if(association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()){
                String sourceClass = association.getSourceClass();
                String sourceRole = association.getSourceRole();
                String sourceMultiplicity = association.getSourceMultiplicity();

                String fieldName = GeneratorUtils.makeAttributeName(sourceClass, sourceRole, sourceMultiplicity);

                if(!generatedRelations.contains(fieldName)){
                    generateRelationshipBaseAnnotations(writer, cls.getName(), sourceClass, fieldName, sourceMultiplicity, association, "source", allClasses);
                    generatedRelations.add(fieldName);
                }
            }
        }
    }

    // para saber se a classe é pai de outra, para gerar @Inheritance
    private boolean isParent(Class cls, List<Class> allClasses) {
        for (Class c : allClasses) {
            if (c.getParentClass() != null && c.getParentClass().getName().equals(cls.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gerar as anotações de relacionamento (@OneToMany, @ManyToOne, @ManyToMany)
     * com base na multiplicidade e navegabilidade da associação.
     */
    private void generateRelationshipBaseAnnotations(PrintWriter writer, String thisClass,
                                                     String otherClass, String fieldName,
                                                     String multiplicity, Association assoc,
                                                     String side, List<Class> allClasses){
        writer.println("    // relationship: " + thisClass + " -> " + otherClass);


        String thisMultiplicity = side.equals("source")
                ? assoc.getSourceMultiplicity()
                : assoc.getTargetMultiplicity();

        String otherMultiplicity = side.equals("source")
                ? assoc.getTargetMultiplicity()
                : assoc.getSourceMultiplicity();

        boolean thisMultiple = GeneratorUtils.checkMultiplicity(thisMultiplicity);
        boolean otherMultiple = GeneratorUtils.checkMultiplicity(otherMultiplicity);
        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

        // Many to Many
        if (thisMultiple && otherMultiple) {
            generateManyToMany(writer, thisClass, otherClass, fieldName, assoc, side, isBidirectional);
        }
        // One to many
        else if (thisMultiple) {
            generateOneToMany(writer, thisClass, otherClass, fieldName, assoc, side, isBidirectional);
        }
        // Many to one
        else if (otherMultiple) {
            generateManyToOne(writer, otherClass, fieldName, assoc);
        }
        // One to one
        else {
            generateOneToOne(writer, thisClass, otherClass, fieldName, assoc, side, isBidirectional);
        }

        writer.println();
    }

    /**
     * @ManyToOne: bidirecional: owner tem @JoinColumn, inverse tem mappedBy
     * unidirecional: @JoinColumn
     * Com qualifier: @MapKey e Map<qualifier, otherClass>
     */
    private void generateManyToMany(PrintWriter writer,
                                    String thisClass,
                                    String otherClass,
                                    String fieldName,
                                    Association assoc,
                                    String side,
                                    boolean isBidirectional) {

        if (isBidirectional) {
            boolean isOwner = side.equals("source");

            if (isOwner) {
                writer.println("    @ManyToMany");
                writer.println("    @JoinTable(");
                writer.println("        name = \"" + thisClass.toLowerCase() + "_" + otherClass.toLowerCase() + "\",");
                writer.println("        joinColumns = @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\"),");
                writer.println("        inverseJoinColumns = @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");
                writer.println("    )");
            } else {
                writer.println("    @ManyToMany(mappedBy = \"" +
                        getOtherSideAttributeName(assoc, side) + "\")");
            }
        } else {
            writer.println("    @ManyToMany");
            writer.println("    @JoinTable(");
            writer.println("        name = \"" + thisClass.toLowerCase() + "_" + otherClass.toLowerCase() + "\",");
            writer.println("        joinColumns = @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\"),");
            writer.println("        inverseJoinColumns = @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");
            writer.println("    )");
        }

        if (assoc.hasQualifier()) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    private List<" + otherClass + "> " + fieldName +
                    " = new ArrayList<>();");
        }
    }

    /**
     * @OneToMany: se for bidirecional, tem mappedBy, senão tem @JoinColumn
     * se for composição, tem cascade all e orphanRemoval
     * se assoc tiver qualifier, é Map<qualifier, otherClass>, senão é List<otherClass>
     */
    private void generateOneToMany(PrintWriter writer,
                                   String thisClass,
                                   String otherClass,
                                   String fieldName,
                                   Association assoc,
                                   String side,
                                   boolean isBidirectional) {

        boolean isComposite = "composite".equals(assoc.getAggregationType());
        String cascade = isComposite ? ", cascade = CascadeType.ALL, orphanRemoval = true" : "";

        if (isBidirectional) {
            writer.println("    @OneToMany(mappedBy = \"" +
                    getOtherSideAttributeName(assoc, side) + "\"" + cascade + ")");
        } else {
            if (isComposite) {
                writer.println("    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)");
            } else {
                writer.println("    @OneToMany");
            }
            writer.println("    @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\")");
        }

        if (assoc.hasQualifier()) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    private List<" + otherClass + "> " + fieldName +
                    " = new ArrayList<>();");
        }
    }

    /**
     * @ManyToOne com @JoinColumn
     * se assoc tiver qualifier, é Map<qualifier, otherClass>, senão é só otherClass
     */
    private void generateManyToOne(PrintWriter writer,
                                   String otherClass,
                                   String fieldName,
                                   Association assoc) {

        writer.println("    @ManyToOne");
        writer.println("    @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");

        if (assoc.hasQualifier()) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }

    /**
     * @OneToOne: se tiver qualifier, é OneToMany, senão é OneToOne normal
     * sem qualifie: owner tem @JoinColumn, inverse tem mappedBy
     * unidirecional: @JoinColumn
     */
    private void generateOneToOne(PrintWriter writer,
                                  String thisClass,
                                  String otherClass,
                                  String fieldName,
                                  Association assoc,
                                  String side,
                                  boolean isBidirectional) {

        // tem qualifier, é OneToMany
        if (assoc.hasQualifier()) {
            if (isBidirectional) {
                writer.println("    @OneToMany(mappedBy = \"" +
                        getOtherSideAttributeName(assoc, side) + "\")");
            } else {
                writer.println("    @OneToMany");
                writer.println("    @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\")");
            }

            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            // Sem qualifier: OneToOne normal
            if (isBidirectional) {
                boolean isOwner = side.equals("source");

                if (isOwner) {
                    writer.println("    @OneToOne");
                    writer.println("    @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");
                } else {
                    writer.println("    @OneToOne(mappedBy = \"" +
                            getOtherSideAttributeName(assoc, side) + "\")");
                }
            } else {
                writer.println("    @OneToOne");
                writer.println("    @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");
            }

            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }

    /**
     * gera os ficheiros para interface UML
     *
     */
    /**
     * Emite as operações da classe.
     * Se tiverem sequence diagram, gera o corpo via OperationGenerator.
     * Caso contrário, gera stub que lança UnsupportedOperationException.
     * Necessário para satisfazer contratos de interfaces e para o menu poder chamar owner.<op>().
     */
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


    // nome do campo do outro lado da associação, para usarmos em mappedBy e @JsonIgnorePropertie
    private String getOtherSideAttributeName(Association assoc, String side) {
        if (side.equals("source")) {
            return GeneratorUtils.makeAttributeName(
                    assoc.getTargetClass(),
                    assoc.getTargetRole(),
                    assoc.getTargetMultiplicity()
            );
        } else {
            return GeneratorUtils.makeAttributeName(
                    assoc.getSourceClass(),
                    assoc.getSourceRole(),
                    assoc.getSourceMultiplicity()
            );
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

}
