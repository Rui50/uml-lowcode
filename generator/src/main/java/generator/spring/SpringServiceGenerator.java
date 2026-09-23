package generator.spring;

import generator.GeneratorUtils;
import generator.OperationGenerator;
import generator.RelationshipInfo;
import model.*;
import model.Class;
import model.annotations.Postcondition;
import model.annotations.PreSnapshot;
import model.annotations.Precondition;
import model.sequence.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Gera um @Service por classe concreta, com:
 *  - CRUD (findAll, findById, create, update/replace, deleteById);
 *  - métodos de relação (assign/add/remove), que trabalham por ids;
 *  - operações custom (dos diagramas de sequência).
 *
 * As invariantes OCL são validadas antes de cada save.
 */

public class SpringServiceGenerator {
    private String baseOutputPath;
    private String basePackage;

    // vars ja declaradads pela preconditions, para evitar redeclaração no corpo do método

    // nome -> Class, para resolver o tipo de id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    // associações do modelo, para traduzir add/remove em coleções nas operações
    private List<Association> associations = new ArrayList<>();

    // nome de DTO -> classe dona, para importar só as folders de DTO que cada service usa
    private final Map<String, String> DTO_ToClass;

    public SpringServiceGenerator(String baseOutputPath, String basePackage, Map<String, String> DTO_ToClass) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
        this.DTO_ToClass = DTO_ToClass;
    }

    /**
    * Gera os ficheiros dos services (CRUD)
    * */
    public void generateServices(List<Class> classes, List<Association> associations, List<Interaction> interactions, List<model.Enumeration> enumerations) throws IOException {
        System.out.println("Generating Spring services...");

        // criar dir
        String serviceDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/service";
        new java.io.File(serviceDir).mkdirs();

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);
        this.associations = associations;

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
            writer.print("public class " + className + "Service");
            if (!cls.getRealizations().isEmpty()) {
                writer.print(" implements ");
                for (int i = 0; i < cls.getRealizations().size(); i++) {
                    if (i > 0) writer.print(", ");
                    writer.print(cls.getRealizations().get(i).getName());
                }
            }
            writer.println(" {");
            writer.println();

            // dependency
            generateDependencies(writer, cls, className, associations, classes);

            // crud methods
            generateCrudMethods(writer, className, cls, associations);

            // metodos de gestao de relação
            generateRelationShipMethods(writer, cls, associations, classes);

            // operações custom (a partir dos diagramas de sequência)
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
        // importa só folders dos DTO que esta classe usa (a própria + as referidas nas ops custom)
        for (String dtoClass : SpringUtils.referencedDTOClasses(cls, classMap.keySet(), DTO_ToClass))
            writer.println("import " + basePackage + ".dto." + dtoClass.toLowerCase() + ".*;");
        for (model.Interface iface : cls.getRealizations()) {
            writer.println("import " + basePackage + ".models." + iface.getName() + ";");
        }
        writer.println();
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.stereotype.Service;");
        writer.println("import org.springframework.transaction.annotation.Transactional;");
        writer.println("import org.springframework.data.domain.Page;");
        writer.println("import org.springframework.data.domain.Pageable;");
        writer.println("import " + basePackage + ".exception.NotFoundException;");
        writer.println();
        writer.println("import java.util.*;");
        writer.println("import java.util.stream.Collectors;");
        writer.println("import java.time.*;");
        writer.println("import java.math.*;");
        writer.println();

        // importar os modelos das entidades se necessário
        List<String> modelsToImport = GeneratorUtils.getModelsToImportFromOperations(cls, classes);
        List<String> modelsToImportFromAssociations = GeneratorUtils.getModelsToImportFromAssociations(cls, associations);
        Set<String> allModelsToImport = new HashSet<>();
        allModelsToImport.addAll(modelsToImport);
        allModelsToImport.addAll(modelsToImportFromAssociations);

        allModelsToImport.addAll(diagramRepoEntities(cls));

        if (!allModelsToImport.isEmpty()) {
            writer.println();
            for (String entityName : allModelsToImport) {
                writer.println("import " + basePackage + ".models." + entityName + ";");
                writer.println("import " + basePackage + ".repository." + entityName + "Repository;");
            }
        }

        // importar todas as enumerações
        if (enumerations != null && !enumerations.isEmpty()) {
            writer.println();
            for (model.Enumeration en : enumerations) {
                writer.println("import " + basePackage + ".models." + en.getName() + ";");
            }
        }
    }

    // dependencias de repositórios inclue o próprio + os das entidades relacionadas
    private void generateDependencies(PrintWriter writer, Class cls, String className, List<Association> associations, List<Class> classes) {
        writer.println("    // Dependencies");
        writer.println("    private final " + className + "Repository " +
                GeneratorUtils.lowercaseFirst(className) + "Repository;");

        // repositories das entidades relacionadas (uma por associação navegável desta classe)
        Set<String> addedRepos = new HashSet<>();
        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            String related = rel.otherClass;
            if (!related.equals(className) && addedRepos.add(related)) {
                writer.println("    private final " + related + "Repository " +
                        GeneratorUtils.lowercaseFirst(related) + "Repository;");
            }
        }

        // repositórios referidos por lifelines nos diagramas mas que nao tem relaçao no diagrama
        for (String entity : diagramRepoEntities(cls)) {
            if (addedRepos.add(entity)) {
                writer.println("    private final " + entity + "Repository " +
                        GeneratorUtils.lowercaseFirst(entity) + "Repository;");
            }
        }

        // services de outras classes chamados nos diagramas das operações
        for (String service : diagramServices(cls))
            writer.println("    private final " + service + " " + GeneratorUtils.lowercaseFirst(service) + ";");

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

        // findAll() — paginado: Page<DTO> a partir de findAll(Pageable) do repositório
        writer.println("    /**");
        writer.println("     * @param pageable paginação/ordenação (query params: page, size, sort)");
        writer.println("     * @return page of entities as DTOs");
        writer.println("     */");
        writer.println("    @Transactional(readOnly = true)");
        writer.println("    public Page<" + SpringDTOGenerator.responseDTOName(cls, "List") + "> findAll(Pageable pageable) {");
        writer.println("        return " + repoName + ".findAll(pageable)");
        writer.println("            .map(" + SpringDTOGenerator.responseDTOName(cls, "List") + "::fromEntity);");
        writer.println("    }");
        writer.println();

        // findById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @return Optional containing the entity DTO if found");
        writer.println("     */");
        writer.println("    @Transactional(readOnly = true)");
        writer.println("    public Optional<" + SpringDTOGenerator.responseDTOName(cls, "Get") + "> findById(" + cls.getRoot().getIdAttributeType() + " id) {");
        writer.println("        return " + repoName + ".findById(id)");
        writer.println("            .map(" + SpringDTOGenerator.responseDTOName(cls, "Get") + "::fromEntity);");
        writer.println("    }");
        writer.println();

        // create()
        // if (!cls.isAbstract())
        generateCreateMethod(writer, className, cls, associations);

        // update() (PATCH parcial) + replace() (PUT full replace)
        generateUpdateMethod(writer, className, cls, associations, false);
        generateUpdateMethod(writer, className, cls, associations, true);

        // deleteById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     */");
        writer.println("    public void deleteById(" + cls.getRoot().getIdAttributeType() + " id) {");
        writer.println("        " + repoName + ".deleteById(id);");
        writer.println("    }");
        writer.println();

        // existsById()
        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @return true if exists");
        writer.println("     */");
        writer.println("    @Transactional(readOnly = true)");
        writer.println("    public boolean existsById(" + cls.getRoot().getIdAttributeType() + " id) {");
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

        // collect() trata da orientação (declarer/herança + auto-assoc bidirecional); aqui só geramos por lado
        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            hasRelationships = generateRelationMethodsForSide(writer, cls, rel, hasRelationships);
        }
    }

    // Gera os métodos de relação para um lado da associação.
    private boolean generateRelationMethodsForSide(PrintWriter writer, Class cls, RelationshipInfo rel,
                                                   boolean hasRelationships) {
        boolean inverseIsCollection = rel.inverseIsCollection();   // o outro lado tem muitos de mim
        boolean isCollection = rel.isCollection();                 // eu tenho muitos deles
        String otherClass = rel.otherClass;
        String attributeName = rel.attributeName();
        // passam para os métodos chamados a seguir (que ainda recebem os params soltos)
        Association assoc = rel.assoc;
        boolean isBidirectional = rel.isBidirectional();
        boolean isSource = rel.isSource;
        String declarer = rel.thisClass();

        // ManyToOne: esta classe tem o FK (lado owner) -> assignX / removeX
        if (inverseIsCollection && !isCollection) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateAssignMethod(writer, cls, otherClass, attributeName, assoc, isBidirectional, isSource, declarer);
            generateRemoveAssignMethod(writer, cls, otherClass, attributeName, assoc, isBidirectional, isSource, declarer);
        }
        // ManyToMany: addX / removeX no lado DONO (@JoinTable)
        else if (inverseIsCollection && isCollection && rel.isOwner()) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateManyToManyAddMethod(writer, cls, otherClass, attributeName, assoc, isBidirectional, isSource, declarer);
            generateManyToManyRemoveMethod(writer, cls, otherClass, attributeName, assoc, isBidirectional, isSource, declarer);
        }
        // OneToMany unidirectional (ou Map qualificado, inclui bidirecional na ponta owner) -> addX / removeX
        else if (rel.hasCollectionEndpoint()) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateOneToManyUniAddMethod(writer, cls, otherClass, attributeName, assoc);
            generateOneToManyUniRemoveMethod(writer, cls, otherClass, attributeName, assoc);
        }
        // 1:1 (nenhum dos lados é coleção) -> assignX / removeX, de cada lado navegável
        else if (!inverseIsCollection && !isCollection) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateOneToOneAssignMethod(writer, cls, rel);
            generateOneToOneRemoveMethod(writer, cls, rel);
        }
        return hasRelationships;
    }

    // nome do campo desta classe do lado de lá (para sincronizar a outra ponta de um 1:1)
    private String inverseAttributeOf(RelationshipInfo rel) {
        return GeneratorUtils.capitalizeFirst(GeneratorUtils.makeAttributeName(
                rel.thisClass(), rel.thisRole(), rel.thisMul()));
    }

    /**
     * 1:1 - assign: liga a relação (sincroniza a outra ponta se for bidirecional).
     */
    private void generateOneToOneAssignMethod(PrintWriter writer, Class cls, RelationshipInfo rel) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherClass = rel.otherClass;
        String otherVar = GeneratorUtils.lowercaseFirst(rel.attributeName());
        String otherRepoName = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";
        String capAttribute = GeneratorUtils.capitalizeFirst(rel.attributeName());
        String capInverse = inverseAttributeOf(rel);

        writer.println();
        writer.println("    /**");
        writer.println("     * OneToOne");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Id the " + otherClass + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO assign" + capAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, " + idTypeOf(otherClass) + " " + otherVar + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        " + otherClass + " " + otherVar + " = " + otherRepoName + ".findById(" + otherVar + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + otherClass +
                " not found: \" + " + otherVar + "Id));");
        writer.println();
        writer.println("        " + varName + ".set" + capAttribute + "(" + otherVar + ");");
        if (rel.isBidirectional()) {
            writer.println("        " + otherVar + ".set" + capInverse + "(" + varName + ");");
        }
        writer.println();
        generateValidation(writer, className, varName);
        if (rel.isBidirectional()) generateValidation(writer, otherClass, otherVar);

        if (rel.isOwner()) {
            writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        } else {
            // a FK está no outro lado, é esse que tem de ser gravado
            writer.println("        " + otherRepoName + ".save(" + otherVar + ");");
            writer.println("        " + className + " saved = " + varName + ";");
        }
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
    }

    /**
     * 1:1 - remove: desliga a relação (limpa a outra ponta se for bidirecional).
     *  StudentMemberService.removeCard(Long studentMemberId)
     */
    private void generateOneToOneRemoveMethod(PrintWriter writer, Class cls, RelationshipInfo rel) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherClass = rel.otherClass;
        String otherVar = GeneratorUtils.lowercaseFirst(rel.attributeName());
        String otherRepoName = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";
        String capAttribute = GeneratorUtils.capitalizeFirst(rel.attributeName());
        String capInverse = inverseAttributeOf(rel);

        writer.println();
        writer.println("    /**");
        writer.println("     * OneToOne");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + capAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();
        writer.println("        " + otherClass + " " + otherVar + " = " + varName + ".get" + capAttribute + "();");
        writer.println("        if (" + otherVar + " != null) {");
        if (rel.isBidirectional()) {
            writer.println("            " + otherVar + ".set" + capInverse + "(null);");
        }
        writer.println("            " + varName + ".set" + capAttribute + "(null);");
        if (!rel.isOwner()) {
            writer.println("            " + otherRepoName + ".save(" + otherVar + ");");
        }
        writer.println("        }");
        writer.println();
        generateValidation(writer, className, varName);
        if (rel.isOwner()) {
            writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        } else {
            writer.println("        " + className + " saved = " + varName + ";");
        }
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
    }

    /**
     * ManyToOne - assign: este lado tem o FK
     *  AlunoService.assignTurno(Long alunoId, Long turnoId)
     * - Procura ambas entidades por ID
     * - Se bidirectional, sincroniza ambos os lados
     * - declarer é a classe que declara a associação e define o nome do campo inverso
     */
    private void generateAssignMethod(PrintWriter writer, Class cls, String otherClass,
                                       String attributeName, Association assoc,
                                       boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        // numa auto-associação o owner e o outro lado são a mesma classe ("employee"), logo as
        // duas variáveis colidiam. Por isso a var/param do outro lado vem do nome do campo
        // ("manager"), não da classe. O repositório continua a vir da classe (employeeRepository).
        String otherVar = GeneratorUtils.lowercaseFirst(attributeName);
        String otherRepoName = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);
        String prevVar = "previous" + capitalizedAttribute;   // dono anterior deste campo numa reatribuição

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Id the " + otherClass + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO assign" + capitalizedAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, " + idTypeOf(otherClass) + " " + otherVar + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        " + otherClass + " " + otherVar + " = " + otherRepoName + ".findById(" + otherVar + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + otherClass +
                " not found: \" + " + otherVar + "Id));");
        writer.println();

        if (isBidirectional) {
            // determinar o nome do campo coleção no outro lado
            String otherSideMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherSideAttribute = GeneratorUtils.makeAttributeName(declarer,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherAttribute = GeneratorUtils.capitalizeFirst(otherSideAttribute);

            // captura o dono anto (para o desligar e validar depois)
            writer.println("        // Remove from previous " + otherClass + " if exists");
            writer.println("        " + otherClass + " " + prevVar + " = " + varName + ".get" + capitalizedAttribute + "();");
            writer.println("        if (" + prevVar + " != null) {");
            // inverso Map  -> put/remove, senão List -> add/remove por valor.
            // esta ponta é a "-para-um" (não é coleção), logo um qualifier só pode fazer Map no inverso
            // por isso basta hasQualifier() para saber que o inverso é Map.
            if (assoc.hasQualifier()) {
                String qKey = varName + ".get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName()) + "()";
                writer.println("            " + prevVar + ".get" + capitalizedOtherAttribute + "().remove(" + qKey + ");");
                writer.println("        }");
                writer.println();
                writer.println("        // Set both sides of the relationship");
                writer.println("        " + varName + ".set" + capitalizedAttribute + "(" + otherVar + ");");
                writer.println("        " + otherVar + ".get" + capitalizedOtherAttribute + "().put(" + qKey + ", " + varName + ");");
            } else {
                writer.println("            " + prevVar + ".get" + capitalizedOtherAttribute + "().remove(" + varName + ");");
                writer.println("        }");
                writer.println();
                writer.println("        // Set both sides of the relationship");
                writer.println("        " + varName + ".set" + capitalizedAttribute + "(" + otherVar + ");");
                writer.println("        " + otherVar + ".get" + capitalizedOtherAttribute + "().add(" + varName + ");");
            }
        } else {
            writer.println("        " + varName + ".set" + capitalizedAttribute + "(" + otherVar + ");");
        }

        writer.println();
        generateValidation(writer, cls.getName(), varName);
        if (isBidirectional) {
            generateValidation(writer, otherClass, otherVar);   // novo outro lado (ganhou este "elem")
            generateValidationIfPresent(writer, otherClass, prevVar);   // dono antigo  (perdeu-o)
        }
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
                                             String attributeName, Association assoc,
                                             boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);

        writer.println("    /**");
        writer.println("     * ManyToOne");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + capitalizedAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        // var do outro lado = nome do campo, não da classe
        String otherVar = GeneratorUtils.lowercaseFirst(attributeName);

        if (isBidirectional) {
            String otherSideMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherSideAttribute = GeneratorUtils.makeAttributeName(declarer,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherAttribute = GeneratorUtils.capitalizeFirst(otherSideAttribute);

            // captura o outro lado (para o remover evalidar depois porque a coleção dele mudou)
            writer.println("        // Remove from both sides");
            writer.println("        " + otherClass + " " + otherVar + " = " + varName + ".get" + capitalizedAttribute + "();");
            writer.println("        if (" + otherVar + " != null) {");
            if (assoc.hasQualifier()) {   // inverso Map -> remove por chave
                writer.println("            " + otherVar + ".get" + capitalizedOtherAttribute + "().remove(" + varName + ".get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName()) + "());");
            } else {
                writer.println("            " + otherVar + ".get" + capitalizedOtherAttribute + "().remove(" + varName + ");");
            }
            writer.println("        }");
        }

        writer.println("        " + varName + ".set" + capitalizedAttribute + "(null);");
        writer.println();
        generateValidation(writer, cls.getName(), varName);
        if (isBidirectional) {
            generateValidationIfPresent(writer, otherClass, otherVar);
        }
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
                                              String attributeName, Association assoc,
                                              boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);
        String otherPlural = otherVar + "s";

        // auto associação (otherClass == className): a var do loop seria "employee", igual à do owner, por isso usa-se "other"
        String elementVar = className.equals(otherClass) ? "other" : otherVar;
        // nome do campo do outro lado que aponta de volta para esta classe (para sincronizar em memória);
        // usa o role deste lado e o isSource diz qual "este" lado.
        String inverseMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
        String inverseAttribute = GeneratorUtils.makeAttributeName(declarer,
                isSource ? assoc.getSourceRole() : assoc.getTargetRole(), inverseMul);
        String inverseCap = GeneratorUtils.capitalizeFirst(inverseAttribute);

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToMany");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO add" + capitalizedAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedAttribute + "().put(" +
                    elementVar + "." + qualifierGetter + "(), " + elementVar + ");");
            if (isBidirectional) {
                // o dono de uma M:N qualificada é sempre a ponta do Map
                // o inverso é List -> sincroniza-se com add, não put.
                writer.println("            " + elementVar + ".get" + inverseCap + "().add(" + varName + ");");
                generateValidationTab(writer, otherClass, elementVar, "            ");   // valida o elemento (o inverso dele mudou)
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedAttribute + "().add(" + elementVar + ");");
            if (isBidirectional) {
                writer.println("            " + elementVar + ".get" + inverseCap + "().add(" + varName + ");");
                generateValidationTab(writer, otherClass, elementVar, "            ");   // valida o elemento (o inverso dele mudou)
            }
            writer.println("        }");
        }

        writer.println();
        generateValidation(writer, cls.getName(), varName);
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
                                                 String attributeName, Association assoc,
                                                 boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);
        String otherPlural = otherVar + "s";

        // auto-associação: "other" para a var do loop não colidir com a do owner
        String elementVar = className.equals(otherClass) ? "other" : otherVar;
        // nome do campo do outro lado que aponta de volta para esta classe (para sincronizar em memória);
        // usa o role deste lado
        String inverseMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
        String inverseAttribute = GeneratorUtils.makeAttributeName(declarer,
                isSource ? assoc.getSourceRole() : assoc.getTargetRole(), inverseMul);
        String inverseCap = GeneratorUtils.capitalizeFirst(inverseAttribute);

        writer.println("    /**");
        writer.println("     * ManyToMany");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + capitalizedAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedAttribute + "().remove(" +
                    elementVar + "." + qualifierGetter + "());");
            if (isBidirectional) {
                // inverso de uma M:N qualificada é List -> remove por VALOR
                writer.println("            " + elementVar + ".get" + inverseCap + "().remove(" + varName + ");");
                generateValidationTab(writer, otherClass, elementVar, "            ");   // valida o elemento (o inverso dele mudou)
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedAttribute + "().remove(" + elementVar + ");");
            if (isBidirectional) {
                writer.println("            " + elementVar + ".get" + inverseCap + "().remove(" + varName + ");");
                generateValidationTab(writer, otherClass, elementVar, "            ");   // valida o elemento (o inverso dele mudou)
            }
            writer.println("        }");
        }

        writer.println();
        generateValidation(writer, cls.getName(), varName);
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
                                                String attributeName, Association assoc) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);
        String otherPlural = otherVar + "s";

        writer.println();
        writer.println("    /**");
        writer.println("     * OneToMany (unidirectional)");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO add" + capitalizedAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedAttribute + "().put(" +
                    otherVar + "." + qualifierGetter + "(), " + otherVar + ");");
            writer.println("        }");
        } else {
            writer.println("        " + varName + ".get" + capitalizedAttribute + "().addAll(" + otherPlural + ");");
        }

        writer.println();
        generateValidation(writer, cls.getName(), varName);
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
                                                   String attributeName, Association assoc) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedAttribute = GeneratorUtils.capitalizeFirst(attributeName);
        String otherPlural = otherVar + "s";

        writer.println("    /**");
        writer.println("     * OneToMany (unidirectional)");
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + capitalizedAttribute +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
            writer.println("        for (" + otherClass + " " + otherVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedAttribute + "().remove(" +
                    otherVar + "." + qualifierGetter + "());");
            writer.println("        }");
        } else {
            writer.println("        " + varName + ".get" + capitalizedAttribute + "().removeIf(" +
                    otherVar + " -> " + otherVar + "Ids.contains(" + otherVar + "." + idGetterOf(otherClass) + "()));");
        }

        writer.println();
        generateValidation(writer, cls.getName(), varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println("        return " + className + "ResponseDTO.fromEntity(saved);");
        writer.println("    }");
        writer.println();
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
        writer.println("    public " + SpringDTOGenerator.responseDTOName(cls, "Create") + " create(" + className + "CreateDTO dto) {");
        writer.println("        " + className + " " + varName + " = dto.toEntity();");
        writer.println();

        // tem dois saves:
        //  1. relações singulares de que é owner (linkOwnedSingles)
        //  2. save() = INSERT, que gera o id desta entidade;
        //  3. ligações que referem esta entidade pelo id (inverse/coleções) «
        //  4. 2º save só quando esta entidade é owner de coleção (M:N) porque aí a ligação só persiste ao gravr
        //     Se a FK está no outro já se gravou lá -> nada a gravar outra vez aqui.
        linkOwnedSingles(writer, cls, associations, varName, true, false);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println();
        boolean inverseModified     = linkInverseSingles(writer, cls, associations, "saved", true, false);
        boolean collectionsModified = linkCollections(writer, cls, associations, "saved");

        // valida com tudo ligado e se rebentar, o @Transactional faz rollback do INSERT
        generateValidation(writer, cls.getName(), "saved");
        if (inverseModified || collectionsModified) {
            writer.println("        saved = " + repoName + ".save(saved);");
        }
        writer.println("        return " + SpringDTOGenerator.responseDTOName(cls, "Create") + ".fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Emite o código que liga um singular de que este lado é dono (@ManyToOne, ou @OneToOne com
     * o @JoinColumn deste lado)
     */
    private boolean linkOwnedSingles(PrintWriter writer, Class cls, List<Association> associations, String entityVar, boolean clearSingleOnNull, boolean forUpdate) {
        boolean selfModified = false;

        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            if (rel.isCollection() || !rel.isOwner()) continue;
            boolean hidden;
            if (forUpdate) {
                hidden = cls.isHiddenInUpdate(rel.attributeName());
            } else {
                hidden = cls.isHiddenInCreate(rel.attributeName());
            }
            if (hidden) continue;

            String otherClass = rel.otherClass;
            String capAttribute   = GeneratorUtils.capitalizeFirst(rel.attributeName());
            // numa auto-associação a var do outro lado colidiria com a do owner ('saved'), por isso vem do campo
            String otherVar   = GeneratorUtils.lowercaseFirst(capAttribute);
            String otherRepo  = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";   // repo já injetado

            writer.println("        if (dto.get" + capAttribute + "Id() != null) {");
            writer.println("            " + otherClass + " " + otherVar + " = " + otherRepo + ".findById(dto.get" + capAttribute + "Id())");
            writer.println("                .orElseThrow(() -> new NotFoundException(\"" + otherClass + " not found: \" + dto.get" + capAttribute + "Id()));");
            writer.println("            " + entityVar + ".set" + capAttribute + "(" + otherVar + ");");
            if (clearSingleOnNull) {
                // gera o "} else { set(null); }" para id omitido limpar a relação (só útil no PUT replace).
                writer.println("        } else {");
                writer.println("            " + entityVar + ".set" + capAttribute + "(null);");
                writer.println("        }");
            } else {
                // update (PATCH): id null/omitido nao mexe na relação, para remover é com o DELETE /{id}/{role}
                writer.println("        }");
            }
            selfModified = true;
        }

        return selfModified;
    }

    /**
     * Liga um singular do lado INVERSO (@OneToOne mappedBy): a FK está na tabela do OUTRO, por isso
     * escreve a referência no outro e grava-o. (Faz o set deste lado também, só para a resposta sair completa.)
     */
    private boolean linkInverseSingles(PrintWriter writer, Class cls, List<Association> associations, String entityVar, boolean clearSingleOnNull, boolean forUpdate) {
        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            if (rel.isCollection() || rel.isOwner()) continue;
            boolean hidden;
            if (forUpdate) {
                hidden = cls.isHiddenInUpdate(rel.attributeName());
            } else {
                hidden = cls.isHiddenInCreate(rel.attributeName());
            }
            if (hidden) continue;

            String otherClass = rel.otherClass;
            String capAttribute   = GeneratorUtils.capitalizeFirst(rel.attributeName());
            String otherVar   = GeneratorUtils.lowercaseFirst(capAttribute);
            String otherRepo  = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";
            // nome do campo desta classe no outro lado da assoc
            String capInv     = GeneratorUtils.capitalizeFirst(GeneratorUtils.makeAttributeName(rel.thisClass(), rel.thisRole(), rel.thisMul()));

            writer.println("        if (dto.get" + capAttribute + "Id() != null) {");
            writer.println("            " + otherClass + " " + otherVar + " = " + otherRepo + ".findById(dto.get" + capAttribute + "Id())");
            writer.println("                .orElseThrow(() -> new NotFoundException(\"" + otherClass + " not found: \" + dto.get" + capAttribute + "Id()));");
            writer.println("            " + entityVar + ".set" + capAttribute + "(" + otherVar + ");");   // sincroniza este lado (resposta)
            writer.println("            " + otherVar + ".set" + capInv + "(" + entityVar + ");");
            generateValidationTab(writer, otherClass, otherVar, "            ");   // gravamos o outro -> validá-lo
            writer.println("            " + otherRepo + ".save(" + otherVar + ");");
            if (clearSingleOnNull) {
                // gera o "} else { ... }": id omitido desliga a relação nos dois lados.
                // quem usa é o PUT
                writer.println("        } else {");
                writer.println("            if (" + entityVar + ".get" + capAttribute + "() != null) {");
                writer.println("                " + entityVar + ".get" + capAttribute + "().set" + capInv + "(null);");
                generateValidationTab(writer, otherClass, entityVar + ".get" + capAttribute + "()", "                ");   // desligamos o antigo -> validá-lo
                writer.println("                " + otherRepo + ".save(" + entityVar + ".get" + capAttribute + "());");
                writer.println("            }");
                writer.println("            " + entityVar + ".set" + capAttribute + "(null);");
                writer.println("        }");
            } else {
                // update (PATCH): id null/omitido nao mexe na relação, remover é com DELETE /{id}/{role}
                writer.println("        }");
            }
        }

        return false;   // só mexemos no OUTRO -> não precisa de dar save
    }

    /**
     * Liga as coleções de uma entidade nova (só é chamado no create).
     *
     *  - 1:N (FK no outro) ou M:N (tabela em conjunto);
     *  - se somos donos: add/put na nossa coleção chega ou senão: liga a referência no outro e grava-o;
     *  - corre DEPOIS do save porque numa M:N o Hibernate rejeitaria uma referência ainda transiente
     *  - devolve true só quando somos donos da coleção
     */

    private boolean linkCollections(PrintWriter writer, Class cls, List<Association> associations, String entityVar) {
        boolean selfModified = false;

        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            if (!rel.isCollection()) continue;
            if (cls.isHiddenInCreate(rel.attributeName())) continue;   // coleções só se ligam no create

            Association assoc = rel.assoc;
            String otherClass = rel.otherClass;
            String capAttribute   = GeneratorUtils.capitalizeFirst(rel.attributeName());
            String otherVar   = GeneratorUtils.lowercaseFirst(capAttribute);
            String otherRepo  = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";

            // se não somos donos, a FK está no outro -> lado inverso
            boolean ownerSide = rel.isOwner();
            // nome do campo desta classe no outro lado , para sincronizar a outra ponta
            String capInv     = GeneratorUtils.capitalizeFirst(GeneratorUtils.makeAttributeName(rel.thisClass(), rel.thisRole(), rel.thisMul()));
            boolean inverseIsCollection = rel.inverseIsCollection();   // o outro lado tem muitos de mim
            boolean thisIsMapEnd = rel.isMap();

            //  'other' na coleção deste lado: put(chave, other) se for Map, senão add(other)
            String addToSaved;
            if (thisIsMapEnd) {
                String qGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
                addToSaved = entityVar + ".get" + capAttribute + "().put(" + otherVar + "." + qGetter + "(), " + otherVar + ");";
            } else {
                addToSaved = entityVar + ".get" + capAttribute + "().add(" + otherVar + ");";
            }

            // só no create: a coleção começa vazia, por isso é só adicionar (sem limpar).
            writer.println("        if (dto.get" + capAttribute + "Ids() != null) {");
            writer.println("            for (" + otherClass + " " + otherVar + " : " + otherRepo + ".findAllById(dto.get" + capAttribute + "Ids())) {");
            // adiciona à coleção deste lado (persiste se formos donos, senão é só para a resposta)
            writer.println("                " + addToSaved);
            if (!ownerSide) {
                // lado inverso logo a FK está no outro -> ligar o campo inversoe gravar o outro
                if (!inverseIsCollection) {
                    // O:M inverso: o outro tem referência singular
                    writer.println("                " + otherVar + ".set" + capInv + "(" + entityVar + ");");
                } else if (assoc.hasQualifier() && !thisIsMapEnd) {
                    // M:N inverso e o Map está do lado do OUTRO -> o campo inverso é put por chave
                    String qInv = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
                    writer.println("                " + otherVar + ".get" + capInv + "().put(" +
                            entityVar + "." + qInv + "(), " + entityVar + ");");
                } else {
                    // M:N inverso: o outro tem coleção (List) desta classe
                    writer.println("                " + otherVar + ".get" + capInv + "().add(" + entityVar + ");");
                }
                generateValidationTab(writer, otherClass, otherVar, "                ");   // gravamos o outro -> validá-lo
                writer.println("                " + otherRepo + ".save(" + otherVar + ");");
            }
            writer.println("            }");
            writer.println("        }");
            if (ownerSide) selfModified = true;   // só gravamos 'saved' quando somos donos da coleção
        }

        return selfModified;
    }

    /**
     * - update() procura a entidade por ID, atualiza os atributos a partir do DTO, valida e guarda
     */
    private void generateUpdateMethod(PrintWriter writer, String className, Class cls, List<Association> associations, boolean fullReplace) {
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String methodName = fullReplace ? "replace" : "update";

        writer.println("    /**");
        writer.println("     * @param id the entity id");
        writer.println("     * @param dto the data to " + (fullReplace ? "replace" : "update"));
        writer.println("     * @return the " + (fullReplace ? "replaced" : "updated") + " entity as DTO");
        writer.println("     */");
        writer.println("    public " + SpringDTOGenerator.responseDTOName(cls, "Update") + " " + methodName + "(" + cls.getRoot().getIdAttributeType() + " id, " + className + "UpdateDTO dto) {");
        writer.println("        " + className + " existing = " + repoName + ".findById(id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className + " not found: \" + id));");
        writer.println();

        // update all attributes from DTO
        writer.println("        dto." + (fullReplace ? "replace" : "update") + "(existing);");
        writer.println();

        // entidade já existe e já tem id -> liga os singulares e grava uma vez (coleções têm endpoints próprios).
        linkOwnedSingles(writer, cls, associations, "existing", fullReplace, true);
        linkInverseSingles(writer, cls, associations, "existing", fullReplace, true);

        // validar depois de ligar
        generateValidation(writer, cls.getName(), "existing");

        // saveAndFlush: força o flush para o @LastModifiedDate (@PreUpdate) ser aplicado
        // antes de mapear para DTO senão o updatedAt na resposta ficaria desatualizado
        writer.println("        " + className + " saved = " + repoName + ".saveAndFlush(existing);");
        writer.println("        return " + SpringDTOGenerator.responseDTOName(cls, "Update") + ".fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Diagramas de sequencia
     * */

    private void generateCustomOperations(PrintWriter writer, Class cls) {
        if (GeneratorUtils.effectiveOperations(cls).isEmpty()) {
            return;
        }

        writer.println("    // operations");

        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob()) continue;   // jobs são gerados como classes próprias em jobs/<Op>Job
            Class owner = GeneratorUtils.declaringClassOf(cls, op);   // classe onde a op (e o diagrama) foi declarada
            Interaction interaction = op.getSequenceDiagram();
            generateCustomOperation(writer, cls, owner, op, interaction);
        }
    }

    private void generateCustomOperation(PrintWriter writer, Class cls, Class owner, Operation op, Interaction interaction) {
        String className = cls.getName();       // classe concreta onde o metodo é emitido (ex: StudentMember)
        String ownerName = owner.getName();     // classe que declara a op / dona do diagrama (ex: Member)
        String operName = op.getName();

        if (interaction == null) {
            // pré/pós-condições precisam de um corpo (self carregado) — sem diagrama não há onde as aplicar
            if (!op.getPreconditions().isEmpty() || !op.getPostconditions().isEmpty()) {
                throw new IllegalArgumentException("Operação '" + operName
                        + "' tem pré/pós-condições mas não tem diagrama de sequência associado.");
            }
            String returnType = op.getReturnType() != null ? GeneratorUtils.removePackageNames(op.getReturnType()) : "void";
            writer.println();
            writer.print("    public " + returnType + " " + operName + "(");
            List<Parameter> params = op.getParameters();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) writer.print(", ");
                writer.print(GeneratorUtils.removePackageNames(params.get(i).getType()) + " " + params.get(i).getName());
            }
            writer.println(") {");
            writer.println("        throw new UnsupportedOperationException(");
            writer.println("            \"Method '" + operName + "' not yet implemented. \"");
            writer.println("        );");
            writer.println("    }");
            return;
        }

        Message entryMessage = SpringUtils.findEntryMessage(interaction, owner, op);
        // operação normal (não-job) tem de ter mensagem de entrada (actor -> service) — define a assinatura
        if (entryMessage == null) {
            throw new IllegalArgumentException("Operação '" + operName
                    + "': o diagrama não tem mensagem de entrada (actor -> " + ownerName + "Service) com o nome da operação.");
        }

        // a mensagem de entrada tem de corresponder à operação declarada (pelo nome)
        if (entryMessage.getName() != null
                && !entryMessage.getName().equals(operName)) {
            throw new IllegalArgumentException(
                    "Diagrama da operação '" + operName + "': a mensagem de entrada '"
                    + entryMessage.getName() + "' não corresponde ao nome da operação.");
        }

        // tipo de retorno: da mensagem de entrada se especificado | senão da operação | senão void
        String returnType;
        if (entryMessage.getReturnType() != null && !entryMessage.getReturnType().isBlank()) {
            returnType = GeneratorUtils.removePackageNames(entryMessage.getReturnType());
        } else if (op.getReturnType() != null) {
            returnType = GeneratorUtils.removePackageNames(op.getReturnType());
        } else {
            returnType = "void";
        }

        // assinatura a partir da mensagem de entrada; o tipo de cada argumento é obrigatório
        writer.println();
        writer.print("    public " + returnType + " " + operName + "(");
        if (!entryMessage.getArguments().isEmpty()) {
            for (int i = 0; i < entryMessage.getArguments().size(); i++) {
                if (i > 0) writer.print(", ");
                MessageArgument arg = entryMessage.getArguments().get(i);
                if (arg.getType() == null || arg.getType().isBlank()) {
                    throw new IllegalArgumentException(
                            "Diagrama da operação '" + operName + "': o argumento '"
                            + arg.getName() + "' não tem tipo (ex: '" + arg.getName() + " : Tipo').");
                }
                writer.print(GeneratorUtils.removePackageNames(arg.getType()) + " " + arg.getName());
            }
        }
        writer.println(") {");

        SequenceBodyGenerator bodyGen = new SequenceBodyGenerator(associations);

        String selfVar = GeneratorUtils.lowercaseFirst(ownerName);   // var/id seguem a classe do diagrama (ex: member, memberId)
        boolean hasConditions = !op.getPreconditions().isEmpty() || !op.getPostconditions().isEmpty();
        // carregamos self se: uma condição o referir (this.), houver snapshot @pre, ou a classe tiver
        // invariantes (verificam-se no fim, DbC). Uma condição só sobre parâmetros (pre: d > 0) não precisa.
        boolean needsSelf =
                cls.hasAnyInvariants()
                || op.getPreconditions().stream().anyMatch(p -> refsSelf(p.getJavaExpression()))
                || op.getPostconditions().stream().anyMatch(p -> refsSelf(p.getJavaExpression())
                        || !p.getPreSnapshots().isEmpty());

        if (needsSelf) {
            // self é carregado a partir de um parâmetro com o nome convencional <entityVar>Id (ex: turmaId)
            String idParam = selfVar + "Id";
            boolean hasIdParam = entryMessage.getArguments().stream()
                    .anyMatch(a -> idParam.equals(a.getName()));
            if (!hasIdParam) {
                throw new IllegalArgumentException("Operação '" + operName + "': condições ou invariantes que referem 'self' "
                        + "precisam de um parâmetro '" + idParam + "' para o carregar.");
            }

            String repoName = GeneratorUtils.lowercaseFirst(className) + "Repository";
            writer.println("        " + ownerName + " " + selfVar + " = " + repoName + ".findById(" + idParam + ")");
            writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className + " not found\"));");
            // pré declara o self (carregado acima) para o SequenceBodyGenerator saltar o
            // "turma = findById(...)" do diagrama e não o declarar duas vezes;
            bodyGen.addPreDeclaredVar(selfVar);
        }

        if (hasConditions) {
            for (Precondition pre : op.getPreconditions()) {
                String javaExpr = pre.getJavaExpression(selfVar);
                writer.println("        if (!(" + javaExpr + ")) {");
                writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(pre.getMessage()) + "\");");
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

        bodyGen.generateMethodBodyFromInteraction(writer, owner, op, interaction, returnType, selfVar);   // lifeline do service = a do diagrama (owner)

        String returnExpr = GeneratorUtils.findReplyToActor(interaction.getElements());
        if ("void".equals(returnType)) {
            // void ent sem reply, invariantes e pos condições ficam no fim
            if (cls.hasAnyInvariants()) {
                writer.println("        " + selfVar + ".checkInvariants();");
            }
            OperationGenerator.generateReturnAndPostconditions(writer, op, returnType, returnExpr, selfVar);
        } else {
            // se nao for void os returns (com invariantes e pos conds) jáa foram postos
            // aqui só validamos que o diagrama tem reply
            OperationGenerator.validateReturn(op, returnType, returnExpr, selfVar);
        }

        writer.println("    }");
    }


    // Helpers

    // tipo do id de uma classe
    private String idTypeOf(String className) {
        return GeneratorUtils.idType(classMap.get(className));
    }

    // getter do id de outra entidade
    private String idGetterOf(String className) {
        return GeneratorUtils.idGetter(classMap.get(className));
    }

    /**
     * entidades em que os repositorios aparecem em lifelines ": XRepository" nos diagramas
     * das operações desta classe. O SequenceBodyGenerator emite chamadas
     * a esses campos, por isso têm de ser injetados E importados
     */
    private Set<String> diagramRepoEntities(Class cls) {
        Set<String> entities = new LinkedHashSet<>();   // ordem determinística
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob() || op.getSequenceDiagram() == null) continue;
            for (Lifeline ll : op.getSequenceDiagram().getLifelines()) {
                String type = ll.getType();
                if (type == null || !type.endsWith("Repository")) continue;
                String entity = type.substring(0, type.length() - "Repository".length());
                if (entity.isEmpty() || entity.equals(cls.getName())) continue;   // o próprio já está declarado
                if (classMap.get(entity) == null) {
                    throw new IllegalArgumentException("Operação '" + op.getName()
                            + "': o diagrama tem uma lifeline ': " + type + "' mas a classe '"
                            + entity + "' não existe no modelo.");
                }
                entities.add(entity);
            }
        }
        return entities;
    }

    // update to check if service actually exists later
    // services de outras classes (lifelines ": XService") nos diagramas das operações desta classe
    private Set<String> diagramServices(Class cls) {
        Set<String> services = new LinkedHashSet<>();
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob() || op.getSequenceDiagram() == null) continue;
            String ownerService = GeneratorUtils.declaringClassOf(cls, op).getName() + "Service";
            for (Lifeline ll : op.getSequenceDiagram().getLifelines())
                if (ll.getType() != null && ll.getType().endsWith("Service") && !ll.getType().equals(ownerService))
                    services.add(ll.getType());
        }
        return services;
    }

    // uma expressão Java traduzida de OCL refere self quando contém "this." (self -> this na tradução)
    private boolean refsSelf(String javaExpr) {
        return javaExpr != null && javaExpr.contains("this.");
    }

    // escreve o <expr>.checkInvariants() se 'className' tiver invariantes
    private void generateValidation(PrintWriter writer, String className, String expr) {
        Class c = classMap.get(className);
        if (c == null || !c.hasAnyInvariants()) return;
        writer.println("        " + expr + ".checkInvariants();");
    }

    //  mas com ident
    private void generateValidationTab(PrintWriter writer, String className, String expr, String ind) {
        Class c = classMap.get(className);
        if (c == null || !c.hasAnyInvariants()) return;
        writer.println(ind + expr + ".checkInvariants();");
    }

    // igual, mas o alvo de validação pode ser null,
    // por ex em assignTurno(alunoId, turnoId), o turno anterior (aluno.getTurno()) é null se o aluno
    // ainda não tinha turno -> não há dono anterior para validar.
    private void generateValidationIfPresent(PrintWriter writer, String className, String var) {
        Class c = classMap.get(className);
        if (c == null || !c.hasAnyInvariants()) return;
        writer.println("        if (" + var + " != null) " + var + ".checkInvariants();");
    }

}
