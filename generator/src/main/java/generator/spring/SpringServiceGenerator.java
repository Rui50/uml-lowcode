package generator.spring;

import generator.GeneratorUtils;
import model.*;
import model.Class;
import model.annotations.Postcondition;
import model.annotations.PreSnapshot;
import model.annotations.Precondition;
import model.sequence.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  Gera um serviço por classe
 *
 *  cada service tem:
 *   - dependencias de outr repos
 *   - CRUD básico (findAll, findById, create, update, deleteById)
 *   - metodos das operações:
 *     Sao handled por ids, em bactch,
 *
 *   Invariantes OCL são validados antes de cada save
 */
public class SpringServiceGenerator {
    private String baseOutputPath;
    private String basePackage;

    // vars ja declaradads pela preconditions, para evitar redeclaração no corpo do método
    private Set<String> preDeclaredVars = new HashSet<>();
    
    public SpringServiceGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    /**
    * Gera os ficheiros dos services (CRUD)
    * */
    public void generateServices(List<Class> classes, List<Association> associations, List<Interaction> interactions, List<model.Enumeration> enumerations) throws IOException {
        System.out.println("Generating Spring services...");

        // criar dir
        String serviceDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/service";
        new java.io.File(serviceDir).mkdirs();

        for (Class cls : classes){
            if (cls.isAbstract()) continue;
            generateService(cls, classes, associations, serviceDir, interactions, enumerations);
        }

        System.out.println("Generation of services completed.");
    }

    // gerar um service
    private void generateService(Class cls, List<Class> classes, List<Association> associations, String serviceDir, List<Interaction> interactions, List<model.Enumeration> enumerations) throws IOException {
        String className = cls.getName();
        String filename = serviceDir + "/" + className + "Service.java";

        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ".service;");
            writer.println();

            // imports
            generateImports(writer, cls, classes, associations, enumerations);

            // class declaration
            writer.println("@Service");
            writer.println("@Transactional");
            writer.println("@RequiredArgsConstructor");
            writer.println("public class " + className + "Service {");
            writer.println();

            // dependency
            generateDependencies(writer, className, associations, classes);

            // crud methods
            generateCrudMethods(writer, className, cls, associations);

            // metodos de gestao de relação
            generateRelationShipMethods(writer, cls, associations, classes);

            // queries base para strings
            //generateQueryMethods(writer, cls);

            // operações do diagrama de classe (TODO: implementar)
            generateCustomOperations(writer, cls);

            writer.println("}");
        }

        System.out.println("Generated: " + className + "Service.java");
    }

    /**
     * Imports de um serviço:
     *  - seu modelo, repo, dtos, e coisas do spring
     *  - add imports de classes refs em operações e entidades associadas
     */
    private void generateImports(PrintWriter writer, Class cls, List<Class> classes, List<Association> associations, List<model.Enumeration> enumerations) {
        // importar a classe do modelo
        writer.println("import " + basePackage + ".models." + cls.getName() + ";");
        writer.println("import " + basePackage + ".repository." + cls.getName() + "Repository;");
        writer.println("import " + basePackage + ".dto." + cls.getName() + "CreateDTO;");
        writer.println("import " + basePackage + ".dto." + cls.getName() + "ResponseDTO;");
        writer.println();
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.stereotype.Service;");
        writer.println("import org.springframework.transaction.annotation.Transactional;");
        writer.println();
        writer.println("import java.util.*;");
        writer.println("import java.util.stream.Collectors;");
        writer.println();

        // importar os modelos das entidades se necessário
        List<String> modelsToImport = GeneratorUtils.getModelsToImportFromOperations(cls, classes);
        List<String> modelsToImportFromAssociations = GeneratorUtils.getModelsToImportFromAssociations(cls, associations);
        Set<String> allModelsToImport = new HashSet<>();
        allModelsToImport.addAll(modelsToImport);
        allModelsToImport.addAll(modelsToImportFromAssociations);

        if (!allModelsToImport.isEmpty()) {
            writer.println();
            for (String entityName : allModelsToImport) {
                writer.println("import " + basePackage + ".models." + entityName + ";");
                writer.println("import " + basePackage + ".repository." + entityName + "Repository;");
            }
        }

        // importar todas as enumerações — podem ser referenciadas no corpo de operações (diagramas de sequência)
        if (enumerations != null && !enumerations.isEmpty()) {
            writer.println();
            for (model.Enumeration en : enumerations) {
                writer.println("import " + basePackage + ".models." + en.getName() + ";");
            }
        }
    }

    // dependencias de repositórios: o próprio + os das entidades
    private void generateDependencies(PrintWriter writer, String className, List<Association> associations, List<Class> classes) {
        writer.println("    // Dependencies");
        writer.println("    private final " + className + "Repository " +
                GeneratorUtils.lowercaseFirst(className) + "Repository;");

        // repositories das entidades relacionadas
        Set<String> addedRepos = new HashSet<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(className) && assoc.isTargetNavigable()) {
                String related = assoc.getTargetClass();
                if (!related.equals(className) && addedRepos.add(related)) {
                    writer.println("    private final " + related + "Repository " +
                            GeneratorUtils.lowercaseFirst(related) + "Repository;");
                }
            }
            if (assoc.getTargetClass().equals(className) && assoc.isSourceNavigable()) {
                String related = assoc.getSourceClass();
                if (!related.equals(className) && addedRepos.add(related)) {
                    writer.println("    private final " + related + "Repository " +
                            GeneratorUtils.lowercaseFirst(related) + "Repository;");
                }
            }
        }

        writer.println();
    }

    /**
     *CRUD: findAll, findById, create, update, deleteById, existsById
      - findAll e findById retornam DTOs (map from entities)
      - create e update recebem DTOs (map to entities)
     */
    private void generateCrudMethods(PrintWriter writer, String className, Class cls, List<Association> associations) {
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";

        writer.println("   // CRUD methods");

        // findAll()
        writer.println("    /**");
        writer.println("     * @return list of all entities as DTOs");
        writer.println("     */");
        writer.println("    public List<" + className + "ResponseDTO> findAll() {");
        writer.println("        return " + repoName + ".findAll().stream()");
        writer.println("            .map(" + className + "ResponseDTO::fromEntity)");
        writer.println("            .collect(Collectors.toList());");
        writer.println("    }");
        writer.println();

        // findById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @return Optional containing the entity DTO if found");
        writer.println("     */");
        writer.println("    public Optional<" + className + "ResponseDTO> findById(Long id) {");
        writer.println("        return " + repoName + ".findById(id)");
        writer.println("            .map(" + className + "ResponseDTO::fromEntity);");
        writer.println("    }");
        writer.println();

        // create() — skip for abstract classes
        if (!cls.isAbstract()) generateCreateMethod(writer, className, cls, associations);

        // update()
        generateUpdateMethod(writer, className, cls);

        // deleteById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     */");
        writer.println("    public void deleteById(Long id) {");
        writer.println("        " + repoName + ".deleteById(id);");
        writer.println("    }");
        writer.println();

        // existsById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @return true if exists");
        writer.println("     */");
        writer.println("    public boolean existsById(Long id) {");
        writer.println("        return " + repoName + ".existsById(id);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Ve os tipos de relações, para cada navegavel, decide a forma
     * (ManyToOne, ManyToMany, OneToMany unidirectional)
     * e manda a função respetiva
     */
    private void generateRelationShipMethods(PrintWriter writer, Class cls, List<Association> associations, List<Class> classes) {
        boolean hasRelationships = false;

        for (Association assoc : associations) {
            boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

            // determinar se esta classe é source ou target na associação
            boolean isSource = cls.isOrInheritsFrom(assoc.getSourceClass());
            boolean isTarget = cls.isOrInheritsFrom(assoc.getTargetClass());

            if (!isSource && !isTarget) continue;

            // multiplicidades do ponto de vista desta classe
            String thisMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherMul = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
            boolean thisMultiple = GeneratorUtils.checkMultiplicity(thisMul);
            boolean otherMultiple = GeneratorUtils.checkMultiplicity(otherMul);

            // navegabilidade: esta classe precisa de ter navegabilidade para o outro lado
            boolean navigableToOther = isSource ? assoc.isTargetNavigable() : assoc.isSourceNavigable();
            if (!navigableToOther) continue;

            String otherClass = isSource ? assoc.getTargetClass() : assoc.getSourceClass();

            // ManyToOne: esta classe tem o FK (lado owner)
            // thisMultiple = muitos de "mim" -> o outro tem coleção desta classe
            // !otherMultiple = eu tenho uma referência singular ao outro
            // Gerar assignX e removeX nesta classe
            if (thisMultiple && !otherMultiple) {
                if (!hasRelationships) {
                    writer.println("    // Relationship stuff");
                    hasRelationships = true;
                }

                String fieldName = GeneratorUtils.makeAttributeName(otherClass,
                        isSource ? assoc.getTargetRole() : assoc.getSourceRole(), "0..1");

                generateAssignMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource);
                generateRemoveAssignMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource);
            }

            // ManyToMany: gerar addX e removeX no lado owner (@JoinTable)
            // No entity generator, o source é o owner
            if (thisMultiple && otherMultiple && isSource) {
                if (!hasRelationships) {
                    writer.println("    // Relationship stuff");
                    hasRelationships = true;
                }

                String fieldName = GeneratorUtils.makeAttributeName(otherClass, assoc.getTargetRole(), otherMul);

                generateManyToManyAddMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional);
                generateManyToManyRemoveMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional);
            }

            // OneToMany unidirectional: esta classe tem a coleção
            // sem back-reference no outro lado
            // Gerar addX e removeX nesta classe
            if ((!thisMultiple || assoc.hasQualifier()) && (otherMultiple || assoc.hasQualifier()) && !isBidirectional) {
                if (!hasRelationships) {
                    writer.println("    // Relationship stuff");
                    hasRelationships = true;
                }

                String fieldName = GeneratorUtils.makeAttributeName(otherClass,
                        isSource ? assoc.getTargetRole() : assoc.getSourceRole(), otherMul);

                generateOneToManyUniAddMethod(writer, cls, otherClass, fieldName, assoc);
                generateOneToManyUniRemoveMethod(writer, cls, otherClass, fieldName, assoc);
            }
        }
    }

    /**
     * ManyToOne - assign: este lado tem o FK
     * Ex: AlunoService.assignTurno(Long alunoId, Long turnoId)
     * - Procura ambas entidades por ID
     * - Se bidirectional, sincroniza ambos os lados
     */
    private void generateAssignMethod(PrintWriter writer, Class cls, String otherClass,
                                       String fieldName, Association assoc,
                                       boolean isBidirectional, boolean isSource) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne: Assign a " + otherClass + " to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Id the " + otherClass + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO assign" + GeneratorUtils.capitalizeFirst(otherClass) +
                "(Long " + varName + "Id, Long " + otherVar + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        " + otherClass + " " + otherVar + " = " + otherRepoName + ".findById(" + otherVar + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + otherClass +
                " not found: \" + " + otherVar + "Id));");
        writer.println();

        if (isBidirectional) {
            // determinar o nome do campo coleção no outro lado
            String otherSideMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherSideField = GeneratorUtils.makeAttributeName(className,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherField = GeneratorUtils.capitalizeFirst(otherSideField);

            writer.println("        // Remove from previous " + otherClass + " if exists");
            writer.println("        if (" + varName + ".get" + capitalizedField + "() != null) {");
            writer.println("            " + varName + ".get" + capitalizedField + "().get" +
                    capitalizedOtherField + "().remove(" + varName + ");");
            writer.println("        }");
            writer.println();
            writer.println("        // Set both sides of the relationship");
            writer.println("        " + varName + ".set" + capitalizedField + "(" + otherVar + ");");
            writer.println("        " + otherVar + ".get" + capitalizedOtherField + "().add(" + varName + ");");
        } else {
            writer.println("        " + varName + ".set" + capitalizedField + "(" + otherVar + ");");
        }

        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * ManyToOne - remove: limpar o FK nesta classe
     * Ex: AlunoService.removeTurno(Long alunoId)
     * - Se bidirectional, remove de ambos os lados
     */
    private void generateRemoveAssignMethod(PrintWriter writer, Class cls, String otherClass,
                                             String fieldName, Association assoc,
                                             boolean isBidirectional, boolean isSource) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);

        writer.println("    /**");
        writer.println("     * ManyToOne: Remove the " + otherClass + " from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + GeneratorUtils.capitalizeFirst(otherClass) +
                "(Long " + varName + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        if (isBidirectional) {
            String otherSideMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherSideField = GeneratorUtils.makeAttributeName(className,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherField = GeneratorUtils.capitalizeFirst(otherSideField);

            writer.println("        // Remove from both sides");
            writer.println("        if (" + varName + ".get" + capitalizedField + "() != null) {");
            writer.println("            " + varName + ".get" + capitalizedField + "().get" +
                    capitalizedOtherField + "().remove(" + varName + ");");
            writer.println("        }");
        }

        writer.println("        " + varName + ".set" + capitalizedField + "(null);");
        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }



    /**
     * ManyToMany - add: adicionar entidades relacionadas por IDs
     * Ex: TurmaService.addAlunos(Long turmaId, List<Long> alunoIds)
     * - Procura todas as entidades por ID
     * - Adiciona à coleção, se bidirectional sincroniza ambos os lados
     */
    private void generateManyToManyAddMethod(PrintWriter writer, Class cls, String otherClass,
                                              String fieldName, Association assoc,
                                              boolean isBidirectional) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPlural = otherVar + "s";

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToMany: Add " + otherClass + " entities to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO add" + GeneratorUtils.capitalizeFirst(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().put(" +
                    otherVar + "." + qualifierGetter + "(), " + otherVar + ");");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = GeneratorUtils.makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + GeneratorUtils.capitalizeFirst(inverseField) + "().put(" +
                        varName + "." + qualifierGetter + "(), " + varName + ");");
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().add(" + otherVar + ");");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = GeneratorUtils.makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + GeneratorUtils.capitalizeFirst(inverseField) + "().add(" + varName + ");");
            }
            writer.println("        }");
        }

        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * ManyToMany - remove: remover entidades relacionadas por IDs
     * Ex: TurmaService.removeAlunos(Long turmaId, List<Long> alunoIds)
     * - Procura as entidades, remove da coleção e sincroniza se bidirectional
     */
    private void generateManyToManyRemoveMethod(PrintWriter writer, Class cls, String otherClass,
                                                 String fieldName, Association assoc,
                                                 boolean isBidirectional) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPlural = otherVar + "s";

        writer.println("    /**");
        writer.println("     * ManyToMany: Remove " + otherClass + " entities from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + GeneratorUtils.capitalizeFirst(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" +
                    otherVar + "." + qualifierGetter + "());");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = GeneratorUtils.makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + GeneratorUtils.capitalizeFirst(inverseField) + "().remove(" +
                        varName + "." + qualifierGetter + "());");
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" + otherVar + ");");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = GeneratorUtils.makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + GeneratorUtils.capitalizeFirst(inverseField) + "().remove(" + varName + ");");
            }
            writer.println("        }");
        }

        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * OneToMany unidirectional - add: esta classe tem a coleção, sem back-reference
     * Ex: TurmaService.addTurnos(Long turmaId, List<Long> turnoIds)
     * - Procura as entidades por ID e adiciona à coleção
     */
    private void generateOneToManyUniAddMethod(PrintWriter writer, Class cls, String otherClass,
                                                String fieldName, Association assoc) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPlural = otherVar + "s";

        writer.println();
        writer.println("    /**");
        writer.println("     * OneToMany (unidirectional): Add " + otherClass + " entities to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO add" + GeneratorUtils.capitalizeFirst(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().put(" +
                    otherVar + "." + qualifierGetter + "(), " + otherVar + ");");
            writer.println("        }");
        } else {
            writer.println("        " + varName + ".get" + capitalizedField + "().addAll(" + otherPlural + ");");
        }

        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * OneToMany unidirectional - remove: remover da coleção por IDs
     * Ex: TurmaService.removeTurnos(Long turmaId, List<Long> turnoIds)
     */
    private void generateOneToManyUniRemoveMethod(PrintWriter writer, Class cls, String otherClass,
                                                   String fieldName, Association assoc) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPlural = otherVar + "s";

        writer.println("    /**");
        writer.println("     * OneToMany (unidirectional): Remove " + otherClass + " entities from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + GeneratorUtils.capitalizeFirst(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" +
                    otherVar + "." + qualifierGetter + "());");
            writer.println("        }");
        } else {
            writer.println("        " + varName + ".get" + capitalizedField + "().removeIf(" +
                    otherVar + " -> " + otherVar + "Ids.contains(" + otherVar + ".getId()));");
        }

        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    private void generateQueryMethods(PrintWriter writer, Class cls) {
        List<Attribute> stringAttributes = cls.getAttributes().stream()
                .filter(attr -> attr.getType().equals("String"))
                .toList();

        if (stringAttributes.isEmpty()) return;

        writer.println("    // Simple query methods for strings");

        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";

        for (Attribute attr : stringAttributes){
            String attrName = attr.getName();
            String capitalizedAttr = attrName.substring(0, 1).toUpperCase() + attrName.substring(1);

            // findByAttribute()
            writer.println("    /**");
            writer.println("     * Find " + className + " by " + attrName);
            writer.println("     * @param " + attrName + " to search by");
            writer.println("     * @return list of matching entities");
            writer.println("     */");
            writer.println("    public List<" + className + "> findBy" + capitalizedAttr +
                    "(String " + attrName + ") {");
            writer.println("        return " + repoName + ".findBy" + capitalizedAttr +
                    "(" + attrName + ");");
            writer.println("    }");
            writer.println();

            // findByAttributeContaining()
            writer.println("    /**");
            writer.println("     * Find " + className + " by " + attrName + " containing text");
            writer.println("     * @param " + attrName + " to search");
            writer.println("     * @return list of matching entities");
            writer.println("     */");
            writer.println("    public List<" + className + "> findBy" + capitalizedAttr +
                    "Containing(String " + attrName + ") {");
            writer.println("        return " + repoName + ".findBy" + capitalizedAttr +
                    "Containing(" + attrName + ");");
            writer.println("    }");
            writer.println();
        }
    }

    /**
     * create() recebe um DTO, converte para entidade, valida invariantes, guarda e retorna DTO
     */
    private void generateCreateMethod(PrintWriter writer, String className, Class cls, List<Association> associations){
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";

        writer.println("    /**");
        writer.println("     * @param dto the data to create the entity");
        writer.println("     * @return the created entity as DTO");
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO create(" + className + "CreateDTO dto) {");
        writer.println("        " + className + " " + varName + " = dto.toEntity();");
        writer.println();
        generateValidationBeforeSave(writer, cls, varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * - update() procura a entidade por ID, atualiza os atributos a partir do DTO, valida e guarda
     */
    private void generateUpdateMethod(PrintWriter writer, String className, Class cls) {
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";

        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @param dto the data to update");
        writer.println("     * @return the updated entity as DTO");
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO update(Long id, " + className + "CreateDTO dto) {");
        writer.println("        " + className + " existing = " + repoName + ".findById(id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className + " not found: \" + id));");
        writer.println();

        // update all attributes from DTO
        List<Attribute> allAttrs = cls.getAllAttributes().stream()
                .filter(a -> !a.hasDerive()).toList();
        if (!allAttrs.isEmpty()) {
            writer.println("        // Update attributes from DTO");
            for (Attribute attr : allAttrs) {
                String attrName = attr.getName();
                String prefix = "boolean".equals(attr.getType()) ? "is" : "get";
                String getter = prefix + GeneratorUtils.capitalizeFirst(attrName);
                String setter = "set" + GeneratorUtils.capitalizeFirst(attrName);

                writer.println("        existing." + setter + "(dto." + getter + "());");
            }
            writer.println();
        }

        generateValidationBeforeSave(writer, cls, "existing");
        writer.println("        " + className + " saved = " + repoName + ".save(existing);");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    // gera a arte de validação de invariantes OCL antes de guardar a entidade, se existirem
    private void generateValidationBeforeSave(PrintWriter writer, Class cls, String varName) {
        if (cls.hasAnyInvariants()) {
            writer.println("        // Validate OCL invariants");
            writer.println("        List<String> errors = " + varName + ".validate();");
            writer.println("        if (!errors.isEmpty()) {");
            writer.println("            throw new IllegalArgumentException(");
            writer.println("                \"Validation failed: \" + String.join(\", \", errors)");
            writer.println("            );");
            writer.println("        }");
            writer.println();
        }
    }

    /**
     * Diagramas de sequencia
     * */

    private void generateCustomOperations(PrintWriter writer, Class cls) {
        if (cls.getOperations().isEmpty()) {
            return;
        }

        writer.println("    // operations (TODO: implement)");

        for (Operation op : cls.getOperations()) {
            Interaction interaction = op.getSequenceDiagram();
            generateCustomOperation(writer, cls, op, interaction);
        }
    }

    private void generateCustomOperation(PrintWriter writer, Class cls, Operation op, Interaction interaction) {
        String className = cls.getName();
        String operName = op.getName();
        String returnType = op.getReturnType() != null ? op.getReturnType() : "void";

        if (interaction == null) {
            writer.println();
            writer.println("    public " + returnType + " " + operName + "() {");
            writer.println("        throw new UnsupportedOperationException(");
            writer.println("            \"Method '" + operName + "' not yet implemented. \"");
            writer.println("        );");
            writer.println("    }");
            return;
        }

        Message entryMessage = SpringUtils.findEntryMessage(interaction, cls, op);

        // assinatura — parâmetros vêm da mensagem de entrada do diagrama de sequência
        writer.println();
        writer.print("    public " + returnType + " " + operName + "(");
        if (entryMessage != null && !entryMessage.getArguments().isEmpty()) {
            for (int i = 0; i < entryMessage.getArguments().size(); i++) {
                if (i > 0) writer.print(", ");
                MessageArgument arg = entryMessage.getArguments().get(i);
                String argType = arg.getType() != null ? arg.getType() : "Long";
                writer.print(argType + " " + arg.getName());
            }
        }
        writer.println(") {");

        preDeclaredVars.clear();

        // preload entity if pre/postcond reference
        String selfVar = GeneratorUtils.lowercaseFirst(className);
        boolean needsSelf = !op.getPreconditions().isEmpty() || !op.getPostconditions().isEmpty();

        if (needsSelf) {
            String repoName = selfVar + "Repository";
            writer.println("        " + className + " " + selfVar + " = " + repoName + ".findById(id)");
            writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className + " not found\"));");
            preDeclaredVars.add(selfVar);

            for (Precondition pre : op.getPreconditions()) {
                String javaExpr = pre.getJavaExpression(selfVar);
                writer.println("        if (!(" + javaExpr + ")) {");
                writer.println("            throw new IllegalStateException(\"" + pre.getMessage() + "\");");
                writer.println("        }");
            }

            // @pre snapshots - declarar uma vez por varName
            Set<String> preSnapshotVars = new HashSet<>();
            for (Postcondition post : op.getPostconditions()) {
                for (PreSnapshot snap : post.getPreSnapshots()) {
                    if (preSnapshotVars.add(snap.getVarName())) {
                        writer.println("        " + snap.getJavaType() + " " + snap.getVarName()
                                + " = " + snap.getJavaInit(selfVar) + ";");
                    }
                }
            }
        }

        generateMethodBodyFromInteraction(writer, cls, op, interaction);

        // a expressão de return vem da reply-message para o actor; sem reply, não é feito return
        String returnExpr = findReplyToActor(interaction.getElements());
        // OCL postcondition pode referir "result"
        // se for o caso, declarar variável "result" para o valor de return
        boolean needsResult = op.getPostconditions().stream()
                .anyMatch(p -> {
                    String je = p.getJavaExpression(selfVar);
                    return je != null && je.matches(".*\\bresult\\b.*");
                });
        if (needsResult && returnExpr != null && !"void".equals(returnType)) {
            writer.println("        " + returnType + " result = " + returnExpr + ";");
        }

        for (Postcondition post : op.getPostconditions()) {
            String javaExpr = post.getJavaExpression(selfVar);
            writer.println("        if (!(" + javaExpr + ")) {");
            writer.println("            throw new IllegalStateException(\"" + post.getMessage() + "\");");
            writer.println("        }");
        }

        if (!"void".equals(returnType) && returnExpr != null) {
            writer.println("        return " + (needsResult ? "result" : returnExpr) + ";");
        }

        writer.println("    }");
    }

    private static String findReplyToActor(List<Object> elements) {
        String last = null;
        for (Object el : elements) {
            if (el instanceof Message m
                    && "reply".equals(m.getMessageSort())
                    && m.getTarget() != null
                    && m.getTarget().getType() == null) {
                String expr = m.getRawName() != null ? m.getRawName() : m.getName();
                if (expr != null && !expr.isBlank()) last = expr.trim();
            } else if (el instanceof Fragment f) {
                for (Operand op : f.getOperands()) {
                    String inner = findReplyToActor(op.getElements());
                    if (inner != null) last = inner;
                }
            }
        }
        return last;
    }

    private void generateMethodBodyFromInteraction(PrintWriter writer, Class cls, Operation op, Interaction
            interaction) {
        // identificar a lifeline do Service (owner deste método)
        String className = cls.getName();
        String serviceName = className + "Service";

        // encontrar a lifeline do service owner
        Lifeline serviceLifeline = null;
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() != null && ll.getType().equals(serviceName)) {
                serviceLifeline = ll;
                break;
            }
        }

        // mapear lifeline ID -> nome da variável java para os repositories
        // ex: TurmaRepository -> turmaRepository, AlunoRepository -> alunoRepository
        Map<String, String> lifelineVarMap = new HashMap<>();
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() != null && ll.getType().endsWith("Repository")) {
                String repoVar = GeneratorUtils.lowercaseFirst(ll.getType());
                lifelineVarMap.put(ll.getId(), repoVar);
            }
            if (ll.getType() != null && ll.getType().endsWith("Service")) {
                lifelineVarMap.put(ll.getId(), "self");
            }
        }

        // percorrer a árvore de elements (Messages e Fragments)
        generateElements(writer, interaction.getElements(), serviceLifeline, lifelineVarMap, 2);
    }

    // percorre a lista de elementos (mensagens ou fragmentos) e gera o código correspondente
    // indent: nível de indentação (0, 1, 2...) — cada nível = 4 espaços
    private void generateElements(PrintWriter writer, List<Object> elements,
                                  Lifeline serviceLifeline, Map<String, String> lifelineVarMap, int indent) {
        String indentStr = "    ".repeat(indent);
        for (Object elem : elements) {
            if (elem instanceof Message) {
                generateMessageCode(writer, (Message) elem, serviceLifeline, lifelineVarMap, indentStr);
            } else if (elem instanceof Fragment) {
                generateFragmentCode(writer, (Fragment) elem, serviceLifeline, lifelineVarMap, indent);
            }
        }
    }

    // gera codigo java para mensagens
    // ignora mensagens que nao fazem parte do service
    private void generateMessageCode(PrintWriter writer, Message msg, Lifeline serviceLifeline,
                                      Map<String, String> lifelineVarMap, String indent) {
        if (msg.getSource() == null || msg.getTarget() == null) return;
        if ("reply".equals(msg.getMessageSort())) return;

        // só processar mensagens enviadas pelo service
        if (serviceLifeline == null || !msg.getSource().getId().equals(serviceLifeline.getId())) return;

        String targetVar = lifelineVarMap.getOrDefault(
                msg.getTarget().getId(),
                GeneratorUtils.lowercaseFirst(msg.getTarget().getType())
        );

        // construir lista dos argumentos
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < msg.getArguments().size(); i++) {
            if (i > 0) args.append(", ");
            args.append(msg.getArguments().get(i).getName());
        }

        String methodName = msg.getName();
        boolean isSelfMessage = msg.getSource().getId().equals(msg.getTarget().getId());

        if (isSelfMessage) {
            // self-message: turma.removeAluno(aluno) -> código direto
            writer.println(indent + methodName + "(" + args + ");");
        } else if (msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
            // chamada com retorno: findById(id) : Turma
            String returnType = msg.getReturnType();
            String varName = msg.getAssignVariable() != null
                    ? msg.getAssignVariable()
                    : GeneratorUtils.lowercaseFirst(returnType);

            // skip cuz this message would duplicate a variable already declared by a precondition check at the start of the method
            if (preDeclaredVars.contains(varName)) return;

            if (methodName.equals("findById")) {
                writer.println(indent + returnType + " " + varName + " = " + targetVar + "." +
                        methodName + "(" + args + ")");
                writer.println(indent + "    .orElseThrow(() -> new RuntimeException(\"" + returnType + " not found\"));");
            } else {
                writer.println(indent + returnType + " " + varName + " = " + targetVar + "." +
                        methodName + "(" + args + ");");
            }
        } else {
            // chamada sem retorno: save(turma)
            writer.println(indent + targetVar + "." + methodName + "(" + args + ");");
        }
    }

    /*
    * gera código para fragmentos (alt, opt, loop)
     */
    private void generateFragmentCode(PrintWriter writer, Fragment frag, Lifeline serviceLifeline,
                                      Map<String, String> lifelineVarMap, int indent) {
        String indentStr = "    ".repeat(indent);
        String operator = frag.getOperator();

        if ("loop".equals(operator)){
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard();

            // guard no formato "var : collection" -> "for (var x : collection)"
            String[] parts = guard.split("\\s*:\\s*", 2);
            if (parts.length == 2) {
                writer.println(indentStr + "for (var " + parts[0].trim() + " : " + parts[1].trim() + ") {");
            } else {
                //fallback: guard é condição booleana
                writer.println(indentStr + "while (" + guard + ") {");
            }
            generateElements(writer, operand.getElements(), serviceLifeline, lifelineVarMap, indent + 1);
            writer.println(indentStr + "}");
        }

        else if ("opt".equals(operator)) {
            Operand operand = frag.getOperands().get(0);
            writer.println(indentStr + "if (" + operand.getGuard() + ") {");
            generateElements(writer, operand.getElements(), serviceLifeline, lifelineVarMap, indent + 1);
            writer.println(indentStr + "}");
        }

        else if ("alt".equals(operator)) {
            for(int i = 0; i < frag.getOperands().size(); i++) {
                Operand operand = frag.getOperands().get(i);
                String guard = operand.getGuard();
                if (i == 0) {
                    writer.println(indentStr + "if (" + guard + ") {");
                } else if (guard != null && !guard.isEmpty() && !guard.equalsIgnoreCase("else")) {
                    writer.println(indentStr + "} else if (" + guard + ") {");
                } else {
                    writer.println(indentStr + "} else {");
                }
                generateElements(writer, operand.getElements(), serviceLifeline, lifelineVarMap, indent + 1);
            }
            writer.println(indentStr + "}");
        }
    }
}
