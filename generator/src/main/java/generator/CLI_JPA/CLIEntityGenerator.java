package generator.CLI_JPA;

import generator.JpaEntityWriter;
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
 * Gera as entidades JPA (CLI-JPA).
 * Para cada classe @Entity/@Table, id (@Id/@GeneratedValue), atributos (@Column/@Enumerated/init inline),
 * relações (@OneToMany/@ManyToOne/@ManyToMany/@MapKey, dono via isOwningSide), getters derivados,
 * getters/setters, toString, validate() (se invariantes) e operações.
 *
 * Notas de design:
 *  - herança: SINGLE_TABLE (discriminador "dtype") ou JOINED; só a raiz leva @Table/@Inheritance;
 *  - id resolvido pela raiz (idAttributeName/idAttributeType, default Long); só no-arg constructor (JPA);
 *  - associação qualificada -> Map<qualifier, outro> (@MapKey); o lado singular nunca é Map.
 *
 */
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
 * id: raiz com idAttributeName == null gera o id por defeito (@Id @GeneratedValue Long id);
 * caso contrário @Id @GeneratedValue vão para o atributo escolhido. Ver model.Class.
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
            writer.println("import java.time.*;");   // atributos de data (LocalDate, etc.)
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
     *  anotações base
     */
    private void generateClassBaseAnnotations(PrintWriter writer, Class cls, List<Class> allClasses) {
        // estratégia de herança
        String strategy = cls.getRootInheritanceStrategy();

        // @Entity
        writer.println("@Entity");

        if (strategy.equals("SINGLE_TABLE") && cls.getParentClass() != null) {
            // para herança single table, só a classe pai tem @Table
            // os outros têm @DiscriminatorValue

            // filho em SINGLE_TABLE: partilha a tabela do pai, por isso não leva @Table.
            // só recebe @DiscriminatorValue, gerado mais abaixo.
            return;
        } else {
            // nome derivado (minúsculas) ou override do painel Persistence (GeneratorUtils.tableName).
            // o override permite contornar nomes que colidem com palavras reservadas (ex: USER)
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

        // no-arg constructor (preciso pelo JPA)
        writer.println();
        writer.println("    public " + className + "() {}");

        // getters and setters for non-derived attributes;
        // se o atributo é o @Id, usar o tipo igual ao campo (que foi alterado ou nao)
        // a wrapper (Integer), senão entity.getX() == null não compila
        for (Attribute attr : cls.getAttributes()) {
            if (attr.hasDerive()) continue;     // derived has its own getter already
            String name = attr.getName();
            String type = name.equals(cls.getIdAttributeName())
                    ? cls.getRoot().getIdAttributeType()
                    : attr.getType();
            String cap  = capitalize(name);
            writer.println();
            writer.println("    public " + type + " get" + cap + "() { return " + name + "; }");
            writer.println("    public void set" + cap + "(" + type + " " + name + ") { this." + name + " = " + name + "; }");
        }

        // id getter/setter só na "raiz" e só para oo id sintético (no custom id o getter
        // do atributo escolhido já é emitido no loop dos atributos)
        if (cls.getParentClass() == null && cls.getIdAttributeName() == null) {
            String idType = cls.getRoot().getIdAttributeType();   // Long (default) ou UUID
            writer.println();
            writer.println("    public " + idType + " getId() { return id; }");
            writer.println("    public void setId(" + idType + " id) { this.id = id; }");
        }

        // getters and setters for relationship fields
        List<String> emittedRelations = new ArrayList<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String otherClass = assoc.getTargetClass();
                String otherRole = assoc.getTargetRole();
                String thisMult = assoc.getSourceMultiplicity();
                String otherMult = assoc.getTargetMultiplicity();
                generateRelationshipGetterSetter(writer, otherClass, otherRole, thisMult, otherMult, assoc, true, emittedRelations);
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String otherClass = assoc.getSourceClass();
                String otherRole = assoc.getSourceRole();
                String thisMult = assoc.getTargetMultiplicity();
                String otherMult = assoc.getSourceMultiplicity();
                generateRelationshipGetterSetter(writer, otherClass, otherRole, thisMult, otherMult, assoc, false, emittedRelations);
            }
        }


        // toString doesnt include relationships for now, avoids infinite cycle
        // se há id custom, o atributo-id já vem do loop senão prefixa com "id=" + getId()
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
        // getAllAttributes: inclui herdados. Via getter — o campo herdado é private no pai
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

    // isSource = a classe que estamos a gerar está na ponta SOURCE desta associação
    private void generateRelationshipGetterSetter(PrintWriter writer,
                                          String otherClass,
                                          String otherRole,
                                          String thisMult,
                                          String otherMult,
                                          Association assoc,
                                          boolean isSource,
                                          List<String> emitted) {
        String fieldName = GeneratorUtils.makeAttributeName(otherClass, otherRole, otherMult);
        if (emitted.contains(fieldName)) return;
        emitted.add(fieldName);

        String type = otherClass;
        // só a ponta que TEM o qualifier vira Map a outra é referência normal.
        if (assoc.isMapEnd(isSource)) {
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

            // atributo escolhido como @Id idAttributeName só é definido na raiz
            // tipo do campo é "fixed" (Integer em vez de int) — JPA não aceita primitivos no @Id
            boolean isId = atr.getName().equals(cls.getIdAttributeName());
            String fieldType = isId ? cls.getRoot().getIdAttributeType() : atr.getType();
            if (isId) {
                writer.println("    @Id");
                // chave natural (String) é atribuída pelo cliente -> sem @GeneratedValue
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
                writer.println("    private " + fieldType + " " + atr.getName() + " = " + javaInit + ";");
            } else {
                writer.println("    private " + fieldType + " " + atr.getName() + ";");
            }
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
                    generateRelationshipBaseAnnotations(writer, cls.getName(), targetClass, fieldName, targetMultiplicity, association, true, allClasses);
                    generatedRelations.add(fieldName);
                }
            }

            if(association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()){
                String sourceClass = association.getSourceClass();
                String sourceRole = association.getSourceRole();
                String sourceMultiplicity = association.getSourceMultiplicity();

                String fieldName = GeneratorUtils.makeAttributeName(sourceClass, sourceRole, sourceMultiplicity);

                if(!generatedRelations.contains(fieldName)){
                    generateRelationshipBaseAnnotations(writer, cls.getName(), sourceClass, fieldName, sourceMultiplicity, association, false, allClasses);
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
                                                     boolean isSource, List<Class> allClasses){
        writer.println("    // relationship: " + thisClass + " -> " + otherClass);

        // thisMultiplicity = multiplicidade do CAMPO desta classe (quantos do outro lado);
        // otherMultiplicity = quantos desta classe é que o outro lado vê
        String thisMultiplicity  = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
        String otherMultiplicity = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();

        boolean thisMultiple = GeneratorUtils.checkMultiplicity(thisMultiplicity);
        boolean otherMultiple = GeneratorUtils.checkMultiplicity(otherMultiplicity);
        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

        // @OrderBy da coleção (persistence tab) — só em campos de coleção (thisMultiple)
        if (thisMultiple) {
            Class self = allClasses.stream().filter(c -> c.getName().equals(thisClass)).findFirst().orElse(null);
            String orderBy = (self != null) ? self.getCollectionOrder().get(fieldName) : null;
            if (orderBy != null) writer.println("    @OrderBy(\"" + orderBy + "\")");
        }

        // writers de relação partilhados com o Spring (JpaEntityWriter): o que sai é JPA puro
        // Many to Many
        if (thisMultiple && otherMultiple) {
            JpaEntityWriter.generateManyToMany(writer, thisClass, otherClass, fieldName, assoc, isSource, isBidirectional);
        }
        // One to many
        else if (thisMultiple) {
            JpaEntityWriter.generateOneToMany(writer, thisClass, otherClass, fieldName, assoc, isSource, isBidirectional);
        }
        // Many to one
        else if (otherMultiple) {
            JpaEntityWriter.generateManyToOne(writer, otherClass, fieldName, assoc);
        }
        // One to one
        else {
            JpaEntityWriter.generateOneToOne(writer, thisClass, otherClass, fieldName, assoc, isSource, isBidirectional);
        }

        writer.println();
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
