package generator.spring;

import generator.GeneratorUtils;
import generator.ValidationGenerator;
import ocl.OCLtoSpringAnnot;
import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.Interface;
import model.Operation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 *
 * Bidirecionalidade: se for bidirecional, gerar @JsonIgnoreProperties para evitar loop infinito na serialização JSON
 *
 * id: raiz com idAttributeName == null gera o id por defeito (@Id @GeneratedValue Long id);
 * caso contrário @Id @GeneratedValue vão para o atributo escolhido. Ver model.Class.
 */
public class SpringEntityGenerator {
    private String baseOutputPath;
    private String basePackage;

    private ValidationGenerator validationGenerator;

    // nomes das enumerações para saber quando um atributo é enum
    private Set<String> enumNames = new HashSet<>();

    public SpringEntityGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    public void generateEntities(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating Spring entities...");

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

    /**
     * Gera um ficheiro Java por interface UML.
     * so assinatura — sem JPA.
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

    // gera os enums como enums Java normais, com @Enumerated nos atributos que os usam
    private void generateEnum(Enumeration enumeration, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + enumeration.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(new File(filename))) {
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

        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            // package
            writer.println("package " + basePackage + ".models;");
            writer.println();

            // imports
            writer.println("import com.fasterxml.jackson.annotation.JsonIgnoreProperties;");
            writer.println("import jakarta.persistence.*;");
            writer.println("import jakarta.validation.constraints.*;");   // anotações Validation (OCL)
            writer.println("import lombok.*;");
            writer.println("import java.util.*;");
            if (cls.getParentClass() != null || GeneratorUtils.isParent(cls, allClasses)) {
                writer.println("import lombok.experimental.SuperBuilder;");
            }
            writer.println();

            // anotações base da classe
            generateClassBaseAnnotations(writer, cls, allClasses);

            // declaração da classe
            writer.print("public " + (cls.isAbstract() ? "abstract " : "") + "class " + cls.getName());
            if (cls.getParentClass() != null) {
                writer.print(" extends " + cls.getParentClass().getName());
            }
            // nota: interfaces UML são realizadas pelo service (anemic entity), não pela entidade
            writer.println(" {");
            writer.println();

            // se nao tem parent gera id
            if (cls.getParentClass() == null) {
                generateId(writer, cls);
            }

            // invariantes OCL simples -> anotações  Validation. as mapeadas saem do validate().
            // se desativado para a classe, fica tudo no validate() (Result vazio)
            OCLtoSpringAnnot.Result constraints = cls.isBeanValidation()
                    ? OCLtoSpringAnnot.map(cls)
                    : new OCLtoSpringAnnot.Result();

            // atributos
            generateAttributes(writer, cls, constraints.annotationsByField);

            // getters derivados
            generateDerivedGetters(writer, cls);

            // relações
            generateRelationships(writer, cls, associations, allClasses);

            validationGenerator.generateValidateMethod(writer, cls, constraints.consumed);

            writer.println("}");

            System.out.println("Generated: " + cls.getName() + ".java");
        }
    }

    /**
     *  anotações base
     */
    private void generateClassBaseAnnotations(PrintWriter writer, Class cls, List<Class> allClasses) {
        // estratégia de herança
        String strategy = cls.getRootInheritanceStrategy();

        // Lombok para nao termos que gerar construtores, getters e setters
        writer.println("@Getter");
        writer.println("@Setter");
        writer.println("@NoArgsConstructor");
        writer.println("@AllArgsConstructor(access = AccessLevel.PRIVATE)");
        // need superbuilder se for de herança
        if (cls.getParentClass() != null || GeneratorUtils.isParent(cls, allClasses)) {
            writer.println("@SuperBuilder");
        } else {
            /***

             * ver isto depois
             *   @Builder.Default
             *   private List<Aluno> alunos = new ArrayList<>();
             *
             *   Without @Builder.Default, Lombok ignores new ArrayList<>() during build and that field can end up null.
             */

            writer.println("@Builder");
        }

        // @Entity
        writer.println("@Entity");

        if (strategy.equals("SINGLE_TABLE") && cls.getParentClass() != null) {
            // para herança single table, só a classe pai tem @Table
            // os têm @DiscriminatorValue
            return;
        } else {
            // @Table , para ja normalizar so para minusculas, depois pensar melhor
            writer.println("@Table(name = \"" + cls.getName().toLowerCase() + "\")");
        }

        // caso seja parent, gerar @Inheritance
        if (cls.getParentClass() == null && GeneratorUtils.isParent(cls, allClasses)) {
            // pai
            if (strategy.equals("SINGLE_TABLE")) {
                writer.println("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)");
                writer.println("@DiscriminatorColumn(name = \"type\")");
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

    // id basico (Long auto-gerado) — só qnd não se define um id custom;
    // se houver id custom, o @Id é colocado no atributo escolhido em generateAttributes
    private void generateId(PrintWriter writer, Class cls) {
        if (cls.getIdAttributeName() != null) return;
        // id sintético: Long (default) ou UUID gerado
        String idType = cls.getRoot().getIdAttributeType();
        writer.println("    @Id");
        if ("UUID".equals(idType)) {
            writer.println("    @GeneratedValue(strategy = GenerationType.UUID)");
            writer.println("    private UUID id;");
        } else {
            writer.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
            writer.println("    private Long id;");
        }
        writer.println();
    }

    private void generateAttributes(PrintWriter writer, Class cls, Map<String, List<String>> annsByField) {
        writer.println("    // attributes");
        for (Attribute atr : cls.getAttributes()){
            // skip derived
            if (atr.hasDerive()) continue;

            // atributo escolhido como @Id (sempre auto-gerado);
            // tipo do campo é boxed (Integer em vez de int) — JPA não aceita primitivos no @Id
            boolean isId = atr.getName().equals(cls.getIdAttributeName());
            String fieldType = isId ? cls.getRoot().getIdAttributeType() : atr.getType();
            if (isId) {
                writer.println("    @Id");
                // chave natural (String) é atribuída pelo cliente -> sem @GeneratedValue;
                // tipos gerados (integrais) mantêm o auto-incremento
                if (!cls.isIdAssigned()) {
                    writer.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
                }
            }

            if (enumNames.contains(atr.getType())) {
                writer.println("    @Enumerated(EnumType.STRING)");
            }
            writer.println("    @Column(name = \"" + atr.getName() + "\")");
            // anotações Bean Validation vindas das invariantes OCL
            for (String ann : annsByField.getOrDefault(atr.getName(), List.of())) {
                writer.println("    " + ann);
            }
            if (atr.hasInit()) {
                String javaInit = atr.getJavaInitExpression();
                writer.println("    @Builder.Default");
                writer.println("    private " + fieldType + " " + atr.getName() + " = " + javaInit + ";");
            } else {
                writer.println("    private " + fieldType + " " + atr.getName() + ";");
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

        // is bidirectional, gerar @JsonIgnoreProperties para evitar loop infinito na serialização JSON
        if (isBidirectional) {
            writer.println("    @JsonIgnoreProperties(\"" + GeneratorUtils.getOtherSideAttributeName(assoc, side) + "\")");
        }

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
                        GeneratorUtils.getOtherSideAttributeName(assoc, side) + "\")");
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
            writer.println("    @Builder.Default");
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    @Builder.Default");
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
                    GeneratorUtils.getOtherSideAttributeName(assoc, side) + "\"" + cascade + ")");
        } else {
            if (isComposite) {
                writer.println("    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)");
            } else {
                writer.println("    @OneToMany");
            }
            writer.println("    @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\")");
        }

        if (assoc.hasQualifier()) {
            writer.println("    @Builder.Default");
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    @Builder.Default");
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
                        GeneratorUtils.getOtherSideAttributeName(assoc, side) + "\")");
            } else {
                writer.println("    @OneToMany");
                writer.println("    @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\")");
            }

            writer.println("    @Builder.Default");
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
                            GeneratorUtils.getOtherSideAttributeName(assoc, side) + "\")");
                }
            } else {
                writer.println("    @OneToOne");
                writer.println("    @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");
            }

            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }



}
