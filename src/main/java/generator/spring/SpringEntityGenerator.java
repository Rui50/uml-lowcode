package generator.spring;

import generator.ValidationGenerator;
import model.Association;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import model.Attribute;
import model.Class;
import model.Association;

public class SpringEntityGenerator {
    private String baseOutputPath;

    // para o tipo de herança, para já user SINGLE_TABLE, depois podemos configurar
    private String inheritanceStrategy = "SINGLE_TABLE";

    private ValidationGenerator validationGenerator;

    public SpringEntityGenerator(String baseOutputPath) {
        this.baseOutputPath = baseOutputPath;
        this.validationGenerator = new ValidationGenerator();
    }

    public void generateEntities(List<Class> classes, List<Association> associations) throws IOException {
        System.out.println("Generating Spring entities...");

        // criar path domain
        String domainPath = baseOutputPath + "/src/main/java/com/example/app/models";
        new File(domainPath).mkdirs();

        for (Class cls : classes) {
            generateEntity(cls, classes, associations, domainPath);
        }

        System.out.println("Entity generation completed.");
    }

    /**
     * gerar uma entidade
     * */

    private void generateEntity(Class cls, List<Class> allClasses, List<Association> associations, String modelsDir) throws IOException {
        String filename = modelsDir + "/" + cls.getName() + ".java";

        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            // package
            writer.println("package " + "com.example.app" + ".models;");
            writer.println();

            // imports
            writer.println("import com.fasterxml.jackson.annotation.JsonIgnoreProperties;");
            writer.println("import jakarta.persistence.*;");
            writer.println("import lombok.*;");
            writer.println("import java.util.*;");
            if (cls.getParentClass() != null || isParent(cls, allClasses)) {
                writer.println("import lombok.experimental.SuperBuilder;");
            }
            writer.println();

            // anotações base da classe
            generateClassBaseAnnotations(writer, cls, allClasses);

            // declaração da classe
            writer.print("public class " + cls.getName());
            if (cls.getParentClass() != null) {
                writer.print(" extends " + cls.getParentClass().getName() + " ");
            }
            writer.println("{");
            writer.println();

            // se nao tem parent gera id
            if (cls.getParentClass() == null) {
                generateId(writer);
            }

            // atributos
            generateAttributes(writer, cls);

            // relações
            generateRelationships(writer, cls, associations, allClasses);

            if (!cls.getInvariants().isEmpty()) {
                validationGenerator.generateValidateMethod(writer, cls);
            }

            writer.println("}");

            System.out.println("Generated: " + cls.getName() + ".java");
        }
    }

    private void generateClassBaseAnnotations(PrintWriter writer, Class cls, List<Class> allClasses) {
        // Lombok para nao termos que gerar construtores, getters e setters
        writer.println("@Getter");
        writer.println("@Setter");
        writer.println("@NoArgsConstructor");
        writer.println("@AllArgsConstructor(access = AccessLevel.PRIVATE)");
        // need superbuilder se for de herança
        if (cls.getParentClass() != null || isParent(cls, allClasses)) {
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
        };

        // @Entity
        writer.println("@Entity");

        if (inheritanceStrategy.equals("SINGLE_TABLE") && cls.getParentClass() != null) {
            // para herança single table, só a classe pai tem @Table
            // os têm @DiscriminatorValue
            return;
        } else {
            // @Table , para ja normalizar so para minusculas, depois pensar melhor
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

    private void generateAttributes(PrintWriter writer, Class cls) {
        writer.println("    // attributes");
        for (Attribute atr : cls.getAttributes()){
            // normalizar nomes?
            writer.println("    @Column(name = \"" + atr.getName() + "\")");
            writer.println("    private " + atr.getType() + " " + atr.getName() + ";");
            writer.println();
        }
    }

    private void generateRelationships(PrintWriter writer, Class cls, List<Association> associations, List<Class> allClasses) {
        List<String> generatedRelations = new ArrayList<>();

        for(Association association : associations) {
            if(association.getSourceClass().equals(cls.getName()) && association.isTargetNavigable()){
                String targetClass = association.getTargetClass();
                String targetRole = association.getTargetRole();
                String targetMultiplicity = association.getTargetMultiplicity();

                String fieldName = makeAttributeName(targetClass, targetRole, targetMultiplicity);

                if(!generatedRelations.contains(fieldName)){
                    generateRelationshipBaseAnnotations(writer, cls.getName(), targetClass,
                            fieldName, targetMultiplicity, association, "target", allClasses);
                    generatedRelations.add(fieldName);
                }
            }

            if(association.getTargetClass().equals(cls.getName()) && association.isSourceNavigable()){
                String sourceClass = association.getSourceClass();
                String sourceRole = association.getSourceRole();
                String sourceMultiplicity = association.getSourceMultiplicity();

                String fieldName = makeAttributeName(sourceClass, sourceRole, sourceMultiplicity);

                if(!generatedRelations.contains(fieldName)){
                    generateRelationshipBaseAnnotations(writer, cls.getName(), sourceClass,
                            fieldName, sourceMultiplicity, association, "source", allClasses);
                    generatedRelations.add(fieldName);
                }
            }
        }
    }


    private boolean isParent(Class cls, List<Class> allClasses) {
        for (Class c : allClasses) {
            if (c.getParentClass() != null && c.getParentClass().getName().equals(cls.getName())) {
                return true;
            }
        }
        return false;
    }

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

        boolean thisMultiple = checkMultiplicity(thisMultiplicity);
        boolean otherMultiple = checkMultiplicity(otherMultiplicity);
        boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

        // is bidirectional, gerar @JsonIgnoreProperties para evitar loop infinito na serialização JSON

        // Many to Many
        if (thisMultiple && otherMultiple) {
            if (isBidirectional) {
                writer.println("    @JsonIgnoreProperties(\"" + getOtherSideAttributeName(assoc, side) + "\")");
            }
            generateManyToMany(writer, thisClass, otherClass, fieldName, assoc, side, isBidirectional);
        }

        // One to many
        else if (thisMultiple) {
            if (isBidirectional) {
                writer.println("    @JsonIgnoreProperties(\"" + getOtherSideAttributeName(assoc, side) + "\")");
            }
            generateOneToMany(writer, thisClass, otherClass, fieldName, assoc, side, isBidirectional);
        }
        // Many to one
        else if (otherMultiple) {
            if (isBidirectional) {
                writer.println("    @JsonIgnoreProperties(\"" + getOtherSideAttributeName(assoc, side) + "\")");
            }
            generateManyToOne(writer, otherClass, fieldName, assoc);
        }

        // One to one
        else {
            if (isBidirectional) {
                writer.println("    @JsonIgnoreProperties(\"" + getOtherSideAttributeName(assoc, side) + "\")");
            }
            generateOneToOne(writer, thisClass, otherClass, fieldName, assoc, side, isBidirectional);
        }

        writer.println();
    }

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

    private void generateOneToMany(PrintWriter writer,
                                   String thisClass,
                                   String otherClass,
                                   String fieldName,
                                   Association assoc,
                                   String side,
                                   boolean isBidirectional) {

        if (isBidirectional) {
            writer.println("    @OneToMany(mappedBy = \"" +
                    getOtherSideAttributeName(assoc, side) + "\")");
        } else {
            writer.println("    @OneToMany");
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
                            getOtherSideAttributeName(assoc, side) + "\")");
                }
            } else {
                writer.println("    @OneToOne");
                writer.println("    @JoinColumn(name = \"" + otherClass.toLowerCase() + "_id\")");
            }

            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }


    private String makeAttributeName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }

        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        return checkMultiplicity(multiplicity) ? base + "s" : base;
    }

    private boolean checkMultiplicity(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }

    private String getOtherSideAttributeName(Association assoc, String side) {
        if (side.equals("source")) {
            return makeAttributeName(
                    assoc.getTargetClass(),
                    assoc.getTargetRole(),
                    assoc.getTargetMultiplicity()
            );
        } else {
            return makeAttributeName(
                    assoc.getSourceClass(),
                    assoc.getSourceRole(),
                    assoc.getSourceMultiplicity()
            );
        }
    }

}
