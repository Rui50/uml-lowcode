package generator.spring;

import model.*;
import model.Class;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpringServiceGenerator {
    public String baseOutputPath;
    public String basePackage = "com.example.app";

    public SpringServiceGenerator(String baseOutputPath) {
        this.baseOutputPath = baseOutputPath;
    }

    /**
    * Gera os ficheiros dos services (CRUD)
    * */
    public void generateServices(List<Class> classes, List<Association> associations) throws IOException {
        System.out.println("Generating Spring services...");

        // criar dir
        String serviceDir = baseOutputPath + "/src/main/java/com/example/app/service";
        new java.io.File(serviceDir).mkdirs();

        for (Class cls : classes){
            generateService(cls, classes, associations, serviceDir);
        }

        System.out.println("Generation of services completed.");
    }

    // gerar um service
    private void generateService(Class cls, List<Class> classes, List<Association> associations, String serviceDir) throws IOException {
        String className = cls.getName();
        String filename = serviceDir + "/" + className + "Service.java";

        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ".service;");
            writer.println();

            // imports
            generateImports(writer, cls, classes, associations);

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

    private void generateImports(PrintWriter writer, Class cls, List<Class> classes, List<Association> associations) {
        // importar a classe do modelo
        writer.println("import " + basePackage + ".models." + cls.getName() + ";");
        writer.println("import " + basePackage + ".repository." + cls.getName() + "Repository;");
        writer.println();
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.stereotype.Service;");
        writer.println("import org.springframework.transaction.annotation.Transactional;");
        writer.println();
        writer.println("import java.util.*;");
        writer.println("import java.util.Optional;");
        writer.println();

        // importar os modelos das entidades se necessário
        List<String> modelsToImport = getModelsToImportFromOperations(cls, classes);
        List<String> modelsToImportFromAssociations = getModelsToImportFromAssociations(cls, associations);
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

    }

    private List<String> getModelsToImportFromOperations(Class cls, List<Class> classes) {
        Set<String> entityNames = new HashSet<>();

        for (Class c : classes) {
            entityNames.add(c.getName());
        }

        Set<String> entitiesToImport = new HashSet<>();

        // nas operações, verificamos returns e params
        for (Operation op : cls.getOperations()) {
            for (Parameter param : op.getParameters()) {
                String baseType = extractBaseType(param.getType());

                if (baseType != null && entityNames.contains(baseType) && !baseType.equals(cls.getName())) {
                    entitiesToImport.add(baseType);
                }
            }

            String returnType = extractBaseType(op.getReturnType());
            if (returnType != null && entityNames.contains(returnType) && !returnType.equals(cls.getName())) {
                entitiesToImport.add(returnType);
            }
        }

        return new ArrayList<>(entitiesToImport);
    }

    private List<String> getModelsToImportFromAssociations(Class cls, List<Association> associations) {
        Set<String> entitiesToImport = new HashSet<>();

        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName())  && assoc.isTargetNavigable()) {
                String relatedClass = assoc.getTargetClass();
                if (!relatedClass.equals(cls.getName())) {
                    entitiesToImport.add(relatedClass);
                }
            }

            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String relatedClass = assoc.getSourceClass();
                if (!relatedClass.equals(cls.getName())) {
                    entitiesToImport.add(relatedClass);
                }
            }
        }

        return new ArrayList<>(entitiesToImport);
    }


    private void generateDependencies(PrintWriter writer, String className, List<Association> associations, List<Class> classes) {
        writer.println("    // Dependencies");
        writer.println("    private final " + className + "Repository " +
                className.toLowerCase() + "Repository;");

        // repositories das entidades relacionadas
        Set<String> addedRepos = new HashSet<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(className) && assoc.isTargetNavigable()) {
                String related = assoc.getTargetClass();
                if (!related.equals(className) && addedRepos.add(related)) {
                    writer.println("    private final " + related + "Repository " +
                            related.toLowerCase() + "Repository;");
                }
            }
            if (assoc.getTargetClass().equals(className) && assoc.isSourceNavigable()) {
                String related = assoc.getSourceClass();
                if (!related.equals(className) && addedRepos.add(related)) {
                    writer.println("    private final " + related + "Repository " +
                            related.toLowerCase() + "Repository;");
                }
            }
        }

        writer.println();
    }

    private void generateCrudMethods(PrintWriter writer, String className, Class cls, List<Association> associations) {
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";

        writer.println("   // CRUD methods");

        // findAll()
        writer.println("    /**");
        writer.println("     * @return list of all entities");
        writer.println("     */");
        writer.println("    public List<" + className + "> findAll() {");
        writer.println("        return " + repoName + ".findAll();");
        writer.println("    }");
        writer.println();

        // findById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @return Optional containing the entity if found");
        writer.println("     */");
        writer.println("    public Optional<" + className + "> findById(Long id) {");
        writer.println("        return " + repoName + ".findById(id);");
        writer.println("    }");
        writer.println();

        // create()
        generateCreateMethod(writer, className, cls, associations);

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

    private void generateRelationShipMethods(PrintWriter writer, Class cls, List<Association> associations, List<Class> classes) {
        boolean hasRelationships = false;

        for (Association assoc : associations) {
            boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

            // determinar se esta classe é source ou target na associação
            boolean isSource = assoc.getSourceClass().equals(cls.getName());
            boolean isTarget = assoc.getTargetClass().equals(cls.getName());

            if (!isSource && !isTarget) continue;

            // multiplicidades do ponto de vista desta classe
            String thisMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherMul = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
            boolean thisMultiple = checkMultiplicity(thisMul);
            boolean otherMultiple = checkMultiplicity(otherMul);

            // navegabilidade: esta classe precisa de ter navegabilidade para o outro lado
            boolean navigableToOther = isSource ? assoc.isTargetNavigable() : assoc.isSourceNavigable();
            if (!navigableToOther) continue;

            String otherClass = isSource ? assoc.getTargetClass() : assoc.getSourceClass();

            // ========================================
            // ManyToOne: esta classe tem o FK (lado owner)
            // thisMultiple = muitos de "mim" (o outro tem coleção de mim)
            // !otherMultiple = eu tenho uma referência singular ao outro
            // Gerar assignX e removeX nesta classe
            // ========================================
            if (thisMultiple && !otherMultiple) {
                if (!hasRelationships) {
                    writer.println("    // Relationship stuff");
                    hasRelationships = true;
                }

                String fieldName = makeAttributeName(otherClass,
                        isSource ? assoc.getTargetRole() : assoc.getSourceRole(), "0..1");

                generateAssignMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource);
                generateRemoveAssignMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource);
            }

            // ========================================
            // ManyToMany: gerar addX e removeX no lado owner (@JoinTable)
            // No entity generator, o source é o owner
            // ========================================
            if (thisMultiple && otherMultiple && isSource) {
                if (!hasRelationships) {
                    writer.println("    // Relationship stuff");
                    hasRelationships = true;
                }

                String fieldName = makeAttributeName(otherClass, assoc.getTargetRole(), otherMul);

                generateManyToManyAddMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional);
                generateManyToManyRemoveMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional);
            }

            // ========================================
            // OneToMany unidirectional: esta classe tem a coleção
            // sem back-reference no outro lado
            // Gerar addX e removeX nesta classe
            // ========================================
            if ((!thisMultiple || assoc.hasQualifier()) && otherMultiple && !isBidirectional) {
                if (!hasRelationships) {
                    writer.println("    // Relationship stuff");
                    hasRelationships = true;
                }

                String fieldName = makeAttributeName(otherClass,
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
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";
        String otherVar = otherClass.toLowerCase();
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = capitalizeFirstLetter(fieldName);

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne: Assign a " + otherClass + " to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Id the " + otherClass + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " assign" + capitalizeFirstLetter(otherClass) +
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
            String otherSideField = makeAttributeName(className,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherField = capitalizeFirstLetter(otherSideField);

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
        writer.println("        return " + repoName + ".save(" + varName + ");");
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
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";
        String capitalizedField = capitalizeFirstLetter(fieldName);

        writer.println("    /**");
        writer.println("     * ManyToOne: Remove the " + otherClass + " from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " remove" + capitalizeFirstLetter(otherClass) +
                "(Long " + varName + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        if (isBidirectional) {
            String otherSideMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherSideField = makeAttributeName(className,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherField = capitalizeFirstLetter(otherSideField);

            writer.println("        // Remove from both sides");
            writer.println("        if (" + varName + ".get" + capitalizedField + "() != null) {");
            writer.println("            " + varName + ".get" + capitalizedField + "().get" +
                    capitalizedOtherField + "().remove(" + varName + ");");
            writer.println("        }");
        }

        writer.println("        " + varName + ".set" + capitalizedField + "(null);");
        writer.println();
        writer.println("        return " + repoName + ".save(" + varName + ");");
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
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";
        String otherVar = otherClass.toLowerCase();
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = capitalizeFirstLetter(fieldName);
        String otherPlural = otherVar + "s";

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToMany: Add " + otherClass + " entities to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " add" + capitalizeFirstLetter(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + capitalizeFirstLetter(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().put(" +
                    otherVar + "." + qualifierGetter + "(), " + otherVar + ");");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + capitalizeFirstLetter(inverseField) + "().put(" +
                        varName + "." + qualifierGetter + "(), " + varName + ");");
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().add(" + otherVar + ");");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + capitalizeFirstLetter(inverseField) + "().add(" + varName + ");");
            }
            writer.println("        }");
        }

        writer.println();
        writer.println("        return " + repoName + ".save(" + varName + ");");
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
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";
        String otherVar = otherClass.toLowerCase();
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = capitalizeFirstLetter(fieldName);
        String otherPlural = otherVar + "s";

        writer.println("    /**");
        writer.println("     * ManyToMany: Remove " + otherClass + " entities from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " remove" + capitalizeFirstLetter(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + capitalizeFirstLetter(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" +
                    otherVar + "." + qualifierGetter + "());");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + capitalizeFirstLetter(inverseField) + "().remove(" +
                        varName + "." + qualifierGetter + "());");
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" + otherVar + ");");
            if (isBidirectional) {
                String inverseMul = assoc.getSourceMultiplicity();
                String inverseField = makeAttributeName(className, assoc.getSourceRole(), inverseMul);
                writer.println("            " + otherVar + ".get" + capitalizeFirstLetter(inverseField) + "().remove(" + varName + ");");
            }
            writer.println("        }");
        }

        writer.println();
        writer.println("        return " + repoName + ".save(" + varName + ");");
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
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";
        String otherVar = otherClass.toLowerCase();
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = capitalizeFirstLetter(fieldName);
        String otherPlural = otherVar + "s";

        writer.println();
        writer.println("    /**");
        writer.println("     * OneToMany (unidirectional): Add " + otherClass + " entities to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " add" + capitalizeFirstLetter(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + capitalizeFirstLetter(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().put(" +
                    otherVar + "." + qualifierGetter + "(), " + otherVar + ");");
            writer.println("        }");
        } else {
            writer.println("        " + varName + ".get" + capitalizedField + "().addAll(" + otherPlural + ");");
        }

        writer.println();
        writer.println("        return " + repoName + ".save(" + varName + ");");
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
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";
        String otherVar = otherClass.toLowerCase();
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = capitalizeFirstLetter(fieldName);
        String otherPlural = otherVar + "s";

        writer.println("    /**");
        writer.println("     * OneToMany (unidirectional): Remove " + otherClass + " entities from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " remove" + capitalizeFirstLetter(otherPlural) +
                "(Long " + varName + "Id, List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + capitalizeFirstLetter(assoc.getQualifierName());
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
        writer.println("        return " + repoName + ".save(" + varName + ");");
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
        String varName = className.toLowerCase();
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

    private void generateCreateMethod(PrintWriter writer, String className, Class cls, List<Association> associations){
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";

        writer.println("    /**");
        writer.println("     * @param " + varName + " the entity to create");
        writer.println("     * @return the created entity");
        writer.println("     */");
        writer.println("    public " + className + " create(" + className + " " + varName + ") {");

        // validações OCL
        if (cls.getInvariants() != null && !cls.getInvariants().isEmpty()) {
            writer.println("        // Validate OCL invariants");
            writer.println("        List<String> errors = " + varName + ".validate();");
            writer.println("        if (!errors.isEmpty()) {");
            writer.println("            throw new IllegalArgumentException(");
            writer.println("                \"Validation failed: \" + String.join(\", \", errors)");
            writer.println("            );");
            writer.println("        }");
            writer.println();
        }

        writer.println("        return " + repoName + ".save(" + varName + ");");
        writer.println("    }");
        writer.println();
    }

    private void generateCustomOperations(PrintWriter writer, Class cls) {
        if (cls.getOperations().isEmpty()) {
            return;
        }

        writer.println("    // Custom operations from class diagram (TODO: implement)");

        for (Operation op : cls.getOperations()) {
            generateCustomOperation(writer, cls, op);
        }
    }

    private void generateUpdateMethod(PrintWriter writer, String className, Class cls) {
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";

        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @param " + varName + " the entity data to update");
        writer.println("     * @return the updated entity");
        writer.println("     */");
        writer.println("    public " + className + " update(Long id, " + className + " " + varName + ") {");
        writer.println("        // Find existing entity");
        writer.println("        " + className + " existing = " + repoName + ".findById(id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className + " not found: \" + id));");
        writer.println();

        // merge atributos , para ja verificamos todos, enquanto nao temos DTOS TODO
        // TODO tipos primitivos nao da para verificar null, vou assumir so o int para ja para testes
        // TODO basicamente o update vai assumir nada null, e depois um PATCH assumo que pode tar null?
        /*writer.println("        // Update attributes");
        for (Attribute attr : cls.getAttributes()) {
            if (attr.getType().equals("int")) continue; // para tipos primitivos, teriamos de usar um wrapper ou outra abordagem para verificar se foi atualizado ou nao
            String attrName = attr.getName();
            String getter = "get" + capitalizeFirstLetter(attrName);
            String setter = "set" + capitalizeFirstLetter(attrName);

            writer.println("        if (" + varName + "." + getter + "() != null) {");
            writer.println("            existing." + setter + "(" + varName + "." + getter + "());");
            writer.println("        }");
        }*/

        if (!cls.getAttributes().isEmpty()) {
            writer.println("        // Update all attributes");
            for (Attribute attr : cls.getAttributes()) {
                String attrName = attr.getName();
                String getter = "get" + capitalizeFirstLetter(attrName);
                String setter = "set" + capitalizeFirstLetter(attrName);

                writer.println("        existing." + setter + "(" + varName + "." + getter + "());");
            }
            writer.println();
        }

        writer.println();
        writer.println("        // para ja coleções nao sao updated aqui");
        writer.println();

        // validações OCL
        if (cls.getInvariants() != null && !cls.getInvariants().isEmpty()) {
            writer.println("        // Validate OCL invariants");
            writer.println("        List<String> errors = existing.validate();");
            writer.println("        if (!errors.isEmpty()) {");
            writer.println("            throw new IllegalArgumentException(");
            writer.println("                \"Validation failed: \" + String.join(\", \", errors)");
            writer.println("            );");
            writer.println("        }");
            writer.println();
        }

        writer.println("        return " + repoName + ".save(existing);");
        writer.println("    }");
        writer.println();
    }

    private void generateCustomOperation(PrintWriter writer, Class cls, Operation op) {
        String operName = op.getName();
        String returnType = removePackageNames(op.getReturnType() != null ? op.getReturnType() : "void");

        // docs
        writer.println("    /**");

        for (Parameter param : op.getParameters()) {
            writer.println("     * @param " + param.getName() + " " + removePackageNames(param.getType()));
        }

        if (!returnType.equals("void")) {
            writer.println("     * @return " + returnType);
        }

        writer.println("     * @throws UnsupportedOperationException until implemented with Sequence Diagram");
        writer.println("     */");

        // assinatura
        writer.print("    public " + returnType + " " + operName + "(");

        // o tamos a gerar so no Generator, nao na classe, mas secalhar teria mais sentido meter o id logo no diagrama
        writer.print("Long id");

        if (!op.getParameters().isEmpty()) {
            writer.print(", ");
            for (int i = 0; i < op.getParameters().size(); i++) {
                Parameter param = op.getParameters().get(i);
                writer.print(removePackageNames(param.getType()) + " " + param.getName());
                if (i < op.getParameters().size() - 1) {
                    writer.print(", ");
                }
            }
        }

        writer.println(") {");
        writer.println("        throw new UnsupportedOperationException(");
        writer.println("            \"Method '" + operName + "' not yet implemented. \"");
        writer.println("        );");
        writer.println("    }");
        writer.println();
    }

    private String removePackageNames(String type) {
        if (type == null) return null;

        //List<Teste.Aluno> -> List<Aluno>

        return type.replaceAll("\\b([A-Za-z_$][\\w$]*\\.)+([A-Za-z_$][\\w$]*)\\b", "$2");
    }

    // extrair o tipo "base" de retorno e parametros
    // simples, nao funciona com tipoes nested, ter de ver com recursividade
    private String extractBaseType(String type) {
        if (type == null) return null;

        type = removePackageNames(type.trim());

        if (type.contains("<")) {
            int start = type.indexOf("<") + 1;
            int end = type.lastIndexOf(">");

            if (start < end) {
                String generic = type.substring(start, end).trim();

                // Map<Integer, Turma> -> Turma
                if (generic.contains(",")) {
                    String[] parts = generic.split(",");
                    return extractBaseType(parts[parts.length - 1].trim());
                }

                // List<Aluno> -> Aluno
                return extractBaseType(generic);
            }
        }

        return type;
    }


    private String getOtherSideAttributeName(Association assoc, boolean isSource, String className) {
        if (isSource) {
            String sourceRole = assoc.getSourceRole();
            if (sourceRole != null && !sourceRole.trim().isEmpty()) {
                return sourceRole.trim();
            }
            return lowercaseFirstLetter(className);
        } else {
            String targetClass = assoc.getTargetClass();
            String targetRole = assoc.getTargetRole();
            String targetMult = assoc.getTargetMultiplicity();

            return makeAttributeName(targetClass, targetRole, targetMult);
        }
    }

    private String makeAttributeName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }

        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        return checkMultiplicity(multiplicity) ? base + "s" : base;
    }

    private boolean isOtherSideCollection(Association assoc, boolean isSource) {
        String inverseMult;
        if (isSource){
            inverseMult = assoc.getSourceMultiplicity();
        } else {
            inverseMult = assoc.getTargetMultiplicity();
        }
        return checkMultiplicity(inverseMult);
    }

    private boolean checkMultiplicity(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String lowercaseFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private Class findClassByName(String name, List<Class> classes) {
        for (Class cls : classes) {
            if (cls.getName().equals(name)) {
                return cls;
            }
        }
        return null;
    }


}
