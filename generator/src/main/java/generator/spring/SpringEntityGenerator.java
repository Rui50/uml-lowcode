package generator.spring;

import generator.JpaEntityWriter;
import generator.GeneratorUtils;
import generator.RelationshipInfo;
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
 *se for bidirecional, gerar @JsonIgnoreProperties para evitar loop infinito na serialização JSON
 */
public class SpringEntityGenerator {
    private String baseOutputPath;
    private String basePackage;

    // adicionar createdAt/updatedAt, classes raiz estendem Auditable
    private final boolean timestamps;

    private ValidationGenerator validationGenerator;

    // nomes das enumerações para saber quando um atributo é enum
    private Set<String> enumNames = new HashSet<>();

    public SpringEntityGenerator(String baseOutputPath, String basePackage, boolean timestamps) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
        this.timestamps = timestamps;
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
     * so assinatura 
     */
    public void generateInterfaces(List<Interface> interfaces) throws IOException {
        if (interfaces == null || interfaces.isEmpty()) return;

        String dir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/models";
        new File(dir).mkdirs();

        for (Interface iface : interfaces) {
            try (PrintWriter w = new PrintWriter(dir + "/" + iface.getName() + ".java")) {
                w.println("package " + basePackage + ".models;");
                w.println();
                w.println("import java.util.*;");
                w.println("import java.time.*;");
                w.println("import java.math.*;");
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
            // atributos de data precisam de java.time
            if (GeneratorUtils.needsJavaTime(cls)) {
                writer.println("import java.time.*;");
            }
            // atributos BigDecimal/BigInteger precisam de java.math
            if (GeneratorUtils.needsJavaMath(cls)) {
                writer.println("import java.math.*;");
            }
            writer.println();

            // anotações base da classe
            generateClassBaseAnnotations(writer, cls, allClasses);

            // declaração da classe
            writer.print("public " + (cls.isAbstract() ? "abstract " : "") + "class " + cls.getName());
            if (cls.getParentClass() != null) {
                writer.print(" extends " + cls.getParentClass().getName());
            } else if (timestamps) {
                // a classe raiz carrega os timestamps filhos herdam
                writer.print(" extends Auditable");
            }
            //interfaces UML são realizadas pelo servic não pela entidade
            writer.println(" {");
            writer.println();

            // se nao tem parent gera id
            if (cls.getParentClass() == null) {
                JpaEntityWriter.generateId(writer, cls);
            }

            // invariantes OCL simples -> anotações  Validation. as mapeadas saem do validate().
            // se desativado para a classe, fica tudo no validate()
            OCLtoSpringAnnot.Result constraints;
            if (cls.isBeanValidation()) {
                constraints = OCLtoSpringAnnot.map(cls, associations);
            } else {
                // vazzio
                constraints = new OCLtoSpringAnnot.Result();
            }

            // atributos
            generateAttributes(writer, cls, constraints.annotationsByAttribute);

            // getters derivados
            JpaEntityWriter.generateDerivedGetters(writer, cls);

            // relações
            generateRelationships(writer, cls, associations, allClasses, constraints.annotationsByAttribute);

            validationGenerator.generateValidateMethod(writer, cls, constraints.consumed);
            validationGenerator.generateCheckInvariantsMethod(writer, cls);

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

        // usar o lombok para nao termos que gerar construtores, getters e setters
        writer.println("@Getter");
        writer.println("@Setter");
        writer.println("@NoArgsConstructor");

        // @Entity
        writer.println("@Entity");

        // filho SINGLE_TABLE partilha a tabela do pai -> sem @Table; os outros levam @Table
        boolean singleTableChild = strategy.equals("SINGLE_TABLE") && cls.getParentClass() != null;
        if (!singleTableChild) {
            // nome derivado (minúsculas) ou override do painel Persistence (GeneratorUtils.tableName)
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

    private void generateAttributes(PrintWriter writer, Class cls, Map<String, List<String>> annsByAttribute) {
        writer.println("    // attributes");
        for (Attribute atr : cls.getAttributes()){
            // skip derived
            if (atr.hasDerive()) continue;

            // coleção (List<String> Set<Long>) @ElementCollection
            if (atr.isCollection()) {
                writer.println("    @ElementCollection");
                for (String ann : annsByAttribute.getOrDefault(atr.getName(), List.of())) {
                    writer.println("    " + ann);
                }
                String init = atr.getCollectionType().equals("List") ? "new ArrayList<>()" : "new HashSet<>()";
                writer.println("    " + atr.getVisibility() + " " + atr.getAttributeType() + " " + atr.getName() + " = " + init + ";");
                writer.println();
                continue;
            }

            // atributo escolhido como @Id
            // tipo do campo é  (Integer em vez de int) porque JPA não aceita primitivos no @Id
            boolean isId = atr.getName().equals(cls.getIdAttributeName());
            String attributeType = isId ? cls.getRoot().getIdAttributeType() : atr.getType();
            if (isId) {
                writer.println("    @Id");
                // chave natural (String) é atribuída pelo cliente -> sem @GeneratedValue;
                // tipos gerados (ints) mantêm o auto-incremento
                if (!cls.isIdAssigned()) {
                    writer.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
                }
            }

            if (enumNames.contains(atr.getType())) {
                writer.println("    @Enumerated(EnumType.STRING)");
            }
            writer.println("    " + GeneratorUtils.columnAnnotation(atr));
            // anotações Bean Validation vindas das invariantes OCL
            for (String ann : annsByAttribute.getOrDefault(atr.getName(), List.of())) {
                writer.println("    " + ann);
            }
            if (atr.hasInit()) {
                String javaInit = atr.getJavaInitExpression();
                writer.println("    " + atr.getVisibility() + " " + attributeType + " " + atr.getName() + " = " + javaInit + ";");
            } else {
                writer.println("    " + atr.getVisibility() + " " + attributeType + " " + atr.getName() + ";");
            }
            writer.println();
        }
    }

    /**
     * Itera as associações navegáveis e emite o campo de relação.
     */
    private void generateRelationships(PrintWriter writer, Class cls, List<Association> associations, List<Class> allClasses,
                                       Map<String, List<String>> annsByAttribute) {
        List<String> generatedRelations = new ArrayList<>();

        for (RelationshipInfo rel : RelationshipInfo.collectOwn(cls, associations)) {
            String attributeName = rel.attributeName();
            if (!generatedRelations.contains(attributeName)) {
                generateRelationshipBaseAnnotations(writer, cls.getName(), rel.otherClass, attributeName, rel.otherMultiplicity, rel.assoc, rel.isSource, allClasses, annsByAttribute);
                generatedRelations.add(attributeName);
            }
        }
    }

    /**
     * Gerar as anotações de relacionamento (@OneToMany, @ManyToOne, @ManyToMany)
     * com base na multiplicidade e navegabilidade da associação.
     */
    private void generateRelationshipBaseAnnotations(PrintWriter writer, String thisClass,
                                                     String otherClass, String attributeName,
                                                     String multiplicity, Association assoc,
                                                     boolean isSource, List<Class> allClasses,
                                                     Map<String, List<String>> annsByAttribute){
        writer.println("    // relationship: " + thisClass + " -> " + otherClass);

        // thisMultiplicity = multiplicidade do atributo desta classe (quantos do outro lado);
        // otherMultiplicity = quantos desta classe é que o outro lado vê
        String thisMultiplicity  = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
        String otherMultiplicity = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();

        // ponta com o Map conta como coleção
        boolean isCollection = GeneratorUtils.checkMultiplicity(thisMultiplicity) || assoc.isMapEnd(isSource);          // o MEU atributo é coleção
        boolean inverseIsCollection = GeneratorUtils.checkMultiplicity(otherMultiplicity) || assoc.isMapEnd(!isSource); // o do outro lado
        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

        // is bidirectional, gerar @JsonIgnoreProperties para evitar loop infinito na serialização JSON
        if (isBidirectional) {
            writer.println("    @JsonIgnoreProperties(\"" + GeneratorUtils.getOtherSideAttributeName(assoc, isSource) + "\")");
        }

        // Bean Validation vinda das invariantes OCL (self.turno <> null -> @NotNull)
        for (String ann : annsByAttribute.getOrDefault(attributeName, List.of())) {
            writer.println("    " + ann);
        }

        // the class being being generated
        Class self = allClasses.stream().filter(c -> c.getName().equals(thisClass)).findFirst().orElseThrow();

        // @OrderBy da coleção (persistence tab) só em campos de coleção
        if (isCollection) {
            String orderBy = self.getCollectionOrder().get(attributeName);
            if (orderBy != null) writer.println("    @OrderBy(\"" + orderBy + "\")");
        }

        // fetch das relações sings (@ManyToOne/@OneToOne) se ausente = LAZY
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



}
