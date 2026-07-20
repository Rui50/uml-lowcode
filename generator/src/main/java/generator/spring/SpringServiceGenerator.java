package generator.spring;

import generator.GeneratorUtils;
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
 *  Gera um serviço por classe
 *
 *  cada service tem:
 *   - dependencias de outr repos
 *   - CRUD básico (findAll, findById, create, update, deleteById)
 *   - metodos das operações:
 *     Sao handled por ids, em bactch,
 *
 *   Invariantes OCL são validados antes de cada save
 *
 *   id: tipo raiz; idTypeOf(nome)/idGetterOf(nome) resolvem o id de OUTRA classe nas relações
 */
public class SpringServiceGenerator {
    private String baseOutputPath;
    private String basePackage;

    // vars ja declaradads pela preconditions, para evitar redeclaração no corpo do método

    // nome -> Class, para resolver o tipo de id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    // associações do modelo, para traduzir add/remove em coleções nas operações
    private List<Association> associations = new ArrayList<>();

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

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);
        this.associations = associations;

        for (Class cls : classes){
            // does not generate service for abstract classes (they are not instantiated)
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
        writer.println("import " + basePackage + ".dto." + cls.getName() + "CreateDTO;");
        writer.println("import " + basePackage + ".dto." + cls.getName() + "UpdateDTO;");
        writer.println("import " + basePackage + ".dto." + cls.getName() + "ResponseDTO;");
        // DTOs de override de response (um por endpoint com override)
        cls.getResponseOverrides().keySet().stream().sorted().forEach(endpointType ->
                writer.println("import " + basePackage + ".dto." + SpringDTOGenerator.responseDTOName(cls, endpointType) + ";"));
        writer.println("import " + basePackage + ".dto.*;");   // params de ops custom podem ser DTOs de qualquer classe
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

        // importar todas as enumerações — podem ser referenciadas no corpo de operações (diagramas de sequência)
        if (enumerations != null && !enumerations.isEmpty()) {
            writer.println();
            for (model.Enumeration en : enumerations) {
                writer.println("import " + basePackage + ".models." + en.getName() + ";");
            }
        }
    }

    // dependencias de repositórios: o próprio + os das entidades relacionadas.
    // inclui associações herdadas de um pai ABSTRATO, cujos métodos de relação são
    // emitidos neste service e por isso precisam do repo do outro lado.
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

        // repositórios referidos por lifelines nos diagramas mas que NÃO são associados
        for (String entity : diagramRepoEntities(cls)) {
            if (addedRepos.add(entity)) {
                writer.println("    private final " + entity + "Repository " +
                        GeneratorUtils.lowercaseFirst(entity) + "Repository;");
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

        // create() — skip for abstract classes
        if (!cls.isAbstract()) generateCreateMethod(writer, className, cls, associations);

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

        for (Association assoc : associations) {
            // classe da hierarquia de cls que DECLARA esta associação (ela ou pai abstrato);
            // null => cls não participa, ou só a herda de um pai concreto (que já gera o seu service).
            String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;

            // normalmente processa-se UMA perspetiva. mas numa auto-associação bidirecional a classe
            // é os DOIS lados, por isso processam-se ambos (source e target) — senão o lado DONO de
            // um M:N self (onde isOwningSide só é true de um lado) nunca seria gerado.
            boolean selfBidirectional = assoc.getSourceClass().equals(assoc.getTargetClass())
                    && assoc.isSourceNavigable() && assoc.isTargetNavigable();
            if (selfBidirectional) {
                RelationshipInfo source = new RelationshipInfo(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity(), assoc, true);
                RelationshipInfo target = new RelationshipInfo(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity(), assoc, false);
                hasRelationships = generateRelationMethodsForSide(writer, cls, source, hasRelationships);
                hasRelationships = generateRelationMethodsForSide(writer, cls, target, hasRelationships);
            } else {
                boolean isSource = assoc.getSourceClass().equals(declarer);
                RelationshipInfo rel = new RelationshipInfo(
                        isSource ? assoc.getTargetClass() : assoc.getSourceClass(),
                        isSource ? assoc.getTargetRole() : assoc.getSourceRole(),
                        isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity(),
                        assoc, isSource);
                hasRelationships = generateRelationMethodsForSide(writer, cls, rel, hasRelationships);
            }
        }
    }

    // Gera os métodos de relação para UMA perspetiva (lado) da associação.
    private boolean generateRelationMethodsForSide(PrintWriter writer, Class cls, RelationshipInfo rel,
                                                   boolean hasRelationships) {
        boolean thisMultiple = rel.thisMultiple();
        boolean otherMultiple = rel.otherMultiple();
        String otherClass = rel.otherClass;
        String fieldName = rel.field();
        // estes 3 alimentam os métodos a jusante (ainda recebem os params crus)
        Association assoc = rel.assoc;
        boolean isBidirectional = rel.isBidirectional();
        boolean isSource = rel.isSource;
        String declarer = rel.thisClass();

        // ManyToOne: esta classe tem o FK (lado owner) -> assignX / removeX
        if (thisMultiple && !otherMultiple) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateAssignMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource, declarer);
            generateRemoveAssignMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource, declarer);
        }
        // ManyToMany: addX / removeX no lado DONO (@JoinTable), decidido por isOwningSide
        else if (thisMultiple && otherMultiple && rel.isOwner()) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateManyToManyAddMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource, declarer);
            generateManyToManyRemoveMethod(writer, cls, otherClass, fieldName, assoc, isBidirectional, isSource, declarer);
        }
        // OneToMany unidirectional (ou Map qualificado, incl. bidirecional na ponta dona) -> addX / removeX
        else if (rel.hasCollectionEndpoint()) {
            if (!hasRelationships) { writer.println("    // Relationship stuff"); hasRelationships = true; }
            generateOneToManyUniAddMethod(writer, cls, otherClass, fieldName, assoc);
            generateOneToManyUniRemoveMethod(writer, cls, otherClass, fieldName, assoc);
        }
        return hasRelationships;
    }

    /**
     * ManyToOne - assign: este lado tem o FK
     * Ex: AlunoService.assignTurno(Long alunoId, Long turnoId)
     * - Procura ambas entidades por ID
     * - Se bidirectional, sincroniza ambos os lados
     * - declarer classe que declara a associação (a própria ou pai abstrato);
     *                 define o nome do campo inverso (ver assocDeclaringClass)
     */
    private void generateAssignMethod(PrintWriter writer, Class cls, String otherClass,
                                       String fieldName, Association assoc,
                                       boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        // numa auto-associação o owner e o outro lado são a mesma classe ("employee"), logo as
        // duas variáveis colidiam. Por isso a var/param do outro lado vem do nome do campo
        // ("manager"), não da classe. O repositório continua a vir da classe (employeeRepository).
        String otherVar = GeneratorUtils.lowercaseFirst(fieldName);
        String otherRepoName = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String prevVar = "previous" + capitalizedField;   // dono anterior deste campo numa reatribuição

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne: Assign a " + otherClass + " to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Id the " + otherClass + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO assign" + capitalizedField +
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
            String otherSideField = GeneratorUtils.makeAttributeName(declarer,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherField = GeneratorUtils.capitalizeFirst(otherSideField);

            // captura o dono ANTIGO (para o desligar E validar depois — perde este elemento)
            writer.println("        // Remove from previous " + otherClass + " if exists");
            writer.println("        " + otherClass + " " + prevVar + " = " + varName + ".get" + capitalizedField + "();");
            writer.println("        if (" + prevVar + " != null) {");
            // inverso Map (qualificado) -> remove/put por chave; senão List -> remove/add por valor
            // basta hasQualifier(): ramo ManyToOne -> esta ponta é non mult,
            // logo o qualifier só pode estar no inverso
            if (assoc.hasQualifier()) {
                String qKey = varName + ".get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName()) + "()";
                writer.println("            " + prevVar + ".get" + capitalizedOtherField + "().remove(" + qKey + ");");
                writer.println("        }");
                writer.println();
                writer.println("        // Set both sides of the relationship");
                writer.println("        " + varName + ".set" + capitalizedField + "(" + otherVar + ");");
                writer.println("        " + otherVar + ".get" + capitalizedOtherField + "().put(" + qKey + ", " + varName + ");");
            } else {
                writer.println("            " + prevVar + ".get" + capitalizedOtherField + "().remove(" + varName + ");");
                writer.println("        }");
                writer.println();
                writer.println("        // Set both sides of the relationship");
                writer.println("        " + varName + ".set" + capitalizedField + "(" + otherVar + ");");
                writer.println("        " + otherVar + ".get" + capitalizedOtherField + "().add(" + varName + ");");
            }
        } else {
            writer.println("        " + varName + ".set" + capitalizedField + "(" + otherVar + ");");
        }

        writer.println();
        generateValidation(writer, cls.getName(), varName);
        if (isBidirectional) {
            generateValidation(writer, otherClass, otherVar);   // novo outro lado (ganhou este "elem")
            generateValidationIfPresent(writer, otherClass, prevVar);   // dono ANTIGO na reatribuição (perdeu-o)
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
                                             String fieldName, Association assoc,
                                             boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);

        writer.println("    /**");
        writer.println("     * ManyToOne: Remove the " + otherClass + " from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + capitalizedField +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println();

        // var do outro lado ome do campo, não da classe
        String otherVar = GeneratorUtils.lowercaseFirst(fieldName);

        if (isBidirectional) {
            String otherSideMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherSideField = GeneratorUtils.makeAttributeName(declarer,
                    isSource ? assoc.getSourceRole() : assoc.getTargetRole(), otherSideMul);
            String capitalizedOtherField = GeneratorUtils.capitalizeFirst(otherSideField);

            // captura o outro lado (para o desligar E validar depois — a coleção dele mudou)
            writer.println("        // Remove from both sides");
            writer.println("        " + otherClass + " " + otherVar + " = " + varName + ".get" + capitalizedField + "();");
            writer.println("        if (" + otherVar + " != null) {");
            if (assoc.hasQualifier()) {   // inverso Map -> remove por chave
                writer.println("            " + otherVar + ".get" + capitalizedOtherField + "().remove(" + varName + ".get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName()) + "());");
            } else {
                writer.println("            " + otherVar + ".get" + capitalizedOtherField + "().remove(" + varName + ");");
            }
            writer.println("        }");
        }

        writer.println("        " + varName + ".set" + capitalizedField + "(null);");
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
                                              String fieldName, Association assoc,
                                              boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPlural = otherVar + "s";
        // Numa AUTO-associação otherClass == className, por isso a var do loop seria "employee",
        // igual à do owner, por isso usa-se "other" só nesse caso, nas
        // associações normais mantém-se o nome baseado na classe.
        String elementVar = className.equals(otherClass) ? "other" : otherVar;
        // nome do campo do OUTRO lado que aponta de volta para esta classe (sincroniza os dois
        // lados em memória). Usa o MEU role, o isSource diz qual é o meu lado.
        String inverseMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
        String inverseField = GeneratorUtils.makeAttributeName(declarer,
                isSource ? assoc.getSourceRole() : assoc.getTargetRole(), inverseMul);
        String inverseCap = GeneratorUtils.capitalizeFirst(inverseField);

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToMany: Add " + otherClass + " entities to " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO add" + capitalizedField +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().put(" +
                    elementVar + "." + qualifierGetter + "(), " + elementVar + ");");
            if (isBidirectional) {
                // numa M:N qualificada só a ponta do qualifier (= esta, o dono) é Map; o inverso é
                // List, por isso sincroniza-se com add e não com put
                writer.println("            " + elementVar + ".get" + inverseCap + "().add(" + varName + ");");
                generateValidationTab(writer, otherClass, elementVar, "            ");   // valida o elemento (o inverso dele mudou)
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().add(" + elementVar + ");");
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
                                                 String fieldName, Association assoc,
                                                 boolean isBidirectional, boolean isSource, String declarer) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String repoName = varName + "Repository";
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherRepoName = otherVar + "Repository";
        String capitalizedField = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPlural = otherVar + "s";
        // auto-associação: "other" p/ a var do loop não colidir com a do owner (mesma classe);
        // associações normais ficam inalteradas. Ver generateManyToManyAddMethod para o porquê.
        String elementVar = className.equals(otherClass) ? "other" : otherVar;
        // nome do campo do OUTRO lado que aponta de volta para esta classe (sincroniza os dois
        // lados em memória). Usa o MEU role, o isSource diz qual é o meu lado.
        String inverseMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
        String inverseField = GeneratorUtils.makeAttributeName(declarer,
                isSource ? assoc.getSourceRole() : assoc.getTargetRole(), inverseMul);
        String inverseCap = GeneratorUtils.capitalizeFirst(inverseField);

        writer.println("    /**");
        writer.println("     * ManyToMany: Remove " + otherClass + " entities from " + className);
        writer.println("     * @param " + varName + "Id the " + className + " id");
        writer.println("     * @param " + otherVar + "Ids list of " + otherClass + " ids to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + "ResponseDTO remove" + capitalizedField +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
                " not found: \" + " + varName + "Id));");
        writer.println("        List<" + otherClass + "> " + otherPlural + " = " + otherRepoName + ".findAllById(" + otherVar + "Ids);");
        writer.println();

        if (assoc.hasQualifier()) {
            String qualifierGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" +
                    elementVar + "." + qualifierGetter + "());");
            if (isBidirectional) {
                // inverso de uma M:N qualificada é List -> remove por VALOR
                writer.println("            " + elementVar + ".get" + inverseCap + "().remove(" + varName + ");");
                generateValidationTab(writer, otherClass, elementVar, "            ");   // valida o elemento (o inverso dele mudou)
            }
            writer.println("        }");
        } else {
            writer.println("        for (" + otherClass + " " + elementVar + " : " + otherPlural + ") {");
            writer.println("            " + varName + ".get" + capitalizedField + "().remove(" + elementVar + ");");
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
        writer.println("    public " + className + "ResponseDTO add" + capitalizedField +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
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
        writer.println("    public " + className + "ResponseDTO remove" + capitalizedField +
                "(" + cls.getRoot().getIdAttributeType() + " " + varName + "Id, List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(" + varName + "Id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className +
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
        generateValidation(writer, cls.getName(), varName);
        writer.println("        " + className + " saved = " + repoName + ".save(" + varName + ");");
        writer.println();
        // liga as relações do DTO; se mexeu na própria entidade (FK/coleção própria), grava de novo
        if (generateApplyRelations(writer, cls, associations, "saved", true, true)) {
            writer.println("        saved = " + repoName + ".save(saved);");
        }
        writer.println("        return " + SpringDTOGenerator.responseDTOName(cls, "Create") + ".fromEntity(saved);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Liga as relações de uma entidade a partir dos ids do DTO. Partilhado por create() e update():
     * entityVar é "saved" no create e "existing" no update. includeCollections distingue os dois:
     * no create ligam-se single refs + coleções; no update só single refs (as coleções são geridas
     * pelos endpoints dedicados add/remove).
     *
     *   - singular lado dono (M:O / O:O dono): entityVar.set<Field>(other); id null -> set(null).
     *   - singular lado inverso (O:O mappedBy): a FK está no outro -> grava-se lá; id null limpa-a.
     *   - coleção (só create): add/put no lado dono; no lado inverso grava-se o outro (que tem a FK).
     *
     * Devolve selfModified = mexeu na própria entidade. Não grava aqui de propósito: o caller faz
     * o save final (create: repo.save; update: já tem saveAndFlush), para não duplicar o save.
     */
    private boolean generateApplyRelations(PrintWriter writer, Class cls, List<Association> associations, String entityVar, boolean includeCollections, boolean clearSingleOnNull) {
        // entityVar é a entidade a ligar ("saved" no create, "existing" no update)
        // selfModified é quando mexemos diretamente em entityVar (caso 1 ou 2) -> o chamador grava
        // anyLinked é quando ligámos alguma relação -> re-validar o estado final antes de gravar
        boolean selfModified = false;
        boolean anyLinked     = false;

        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            // 'saved' é instância de cls (subclasse), mas o campo é herdado de declarer.
            Association assoc  = rel.assoc;
            boolean isSource   = rel.isSource;
            String declarer    = rel.thisClass();

            String otherClass  = rel.otherClass;
            // nome do campo = o mesmo que o CreateDTO/entidade usam, por isso get<Field>Id()/Ids() existem
            String capField    = GeneratorUtils.capitalizeFirst(rel.field());
            boolean collection = rel.isCollection();
            // coleções só são ligadas no create; em update são geridas pelos endpoints add/remove dedicados
            if (collection && !includeCollections) continue;
            // relação escondida do DTO que este metodo le (create->CreateDTO, update->UpdateDTO):
            // o campo get<Field>Id()/Ids() não existe, logo não se liga aqui
            if (includeCollections ? cls.isHiddenInCreate(rel.field()) : cls.isHiddenInUpdate(rel.field())) continue;

            // dados do MEU lado — preciso para o campo inverso (caso 3)
            String thisRole = rel.thisRole();
            String thisMul  = rel.thisMul();
            boolean thisMultiple = rel.thisMultiple();

            // mesma razão do assign: numa auto-associação a var do outro lado seria igual à do
            // owner ('saved'/'employee'), por isso vem do nome do campo ("manager"). Repo da classe.
            String otherVar  = GeneratorUtils.lowercaseFirst(capField);
            String otherRepo = GeneratorUtils.lowercaseFirst(otherClass) + "Repository";   // repo já injetado (entidade associada)
            anyLinked = true;

            // caso 1 referência singular (ManyToOne — esta tem a FK; ou OneToOne)
            if (!collection) {
                boolean ownerSide = GeneratorUtils.isOwningSide(assoc, isSource);
                writer.println("        if (dto.get" + capField + "Id() != null) {");
                writer.println("            " + otherClass + " " + otherVar + " = " + otherRepo + ".findById(dto.get" + capField + "Id())");
                writer.println("                .orElseThrow(() -> new NotFoundException(\"" + otherClass + " not found: \" + dto.get" + capField + "Id()));");
                writer.println("            " + entityVar + ".set" + capField + "(" + otherVar + ");");   // sincroniza este lado (resposta)
                if (ownerSide) {
                    if (clearSingleOnNull) {
                        // clear-on-null: create é no-op (campo já null); PUT replace limpa a relação
                        writer.println("        } else {");
                        writer.println("            " + entityVar + ".set" + capField + "(null);");
                        writer.println("        }");
                    } else {
                        // update (PATCH): id null/omitido NÃO mexe na relação; limpar é via DELETE /{id}/{role}
                        writer.println("        }");
                    }
                    selfModified = true;   // a FK pode ter mudado -> re-gravar entityVar
                } else {
                    // OneToOne lado inverso (mappedBy): a FK está no outro -> escreve-se lá e grava-se o outro
                    String capInv = GeneratorUtils.capitalizeFirst(GeneratorUtils.makeAttributeName(declarer, thisRole, thisMul));
                    writer.println("            " + otherVar + ".set" + capInv + "(" + entityVar + ");");
                    generateValidationTab(writer, otherClass, otherVar, "            ");   // gravamos o outro -> validá-lo
                    writer.println("            " + otherRepo + ".save(" + otherVar + ");");
                    if (clearSingleOnNull) {
                        // clear-on-null: create é no-op; PUT replace desliga a relação inversa
                        writer.println("        } else {");
                        writer.println("            if (" + entityVar + ".get" + capField + "() != null) {");
                        writer.println("                " + entityVar + ".get" + capField + "().set" + capInv + "(null);");
                        generateValidationTab(writer, otherClass, entityVar + ".get" + capField + "()", "                ");   // desligamos o antigo -> validá-lo
                        writer.println("                " + otherRepo + ".save(" + entityVar + ".get" + capField + "());");
                        writer.println("            }");
                        writer.println("            " + entityVar + ".set" + capField + "(null);");
                        writer.println("        }");
                    } else {
                        // update (PATCH): id null/omitido NÃO mexe na relação; limpar é via DELETE /{id}/{role}
                        writer.println("        }");
                    }
                }
            }
            // casos2/3 é quando é coleção
            else {
                // dono da coleção segundo a NAVEGABILIDADE (mesma decisão da entidade);
                // se não somos donos, a FK está no outro -> lado inverso
                boolean ownerSide = GeneratorUtils.isOwningSide(assoc, isSource);
                // nome do campo desta classe NO OUTRO lado (back-reference) usado no inverso e no
                // dono M:N bidirecional (para sincronizar/desligar a outra ponta)
                String capInv = GeneratorUtils.capitalizeFirst(GeneratorUtils.makeAttributeName(declarer, thisRole, thisMul));

                boolean thisIsMapEnd = assoc.isMapEnd(isSource);

                // como adicionar 'other' à coleção deste lado: put se for Map qualificado, senão add
                String addToSaved;
                if (thisIsMapEnd) {
                    String qGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
                    addToSaved = entityVar + ".get" + capField + "().put(" + otherVar + "." + qGetter + "(), " + otherVar + ");";
                } else {
                    addToSaved = entityVar + ".get" + capField + "().add(" + otherVar + ");";
                }

                // só no create: a coleção começa vazia, por isso é só adicionar (sem limpar).
                writer.println("        if (dto.get" + capField + "Ids() != null) {");
                writer.println("            for (" + otherClass + " " + otherVar + " : " + otherRepo + ".findAllById(dto.get" + capField + "Ids())) {");
                // adiciona à coleção deste lado (persiste se formos donos, senão é só para a resposta)
                writer.println("                " + addToSaved);
                if (!ownerSide) {
                    // lado INVERSO logo a FK está no outro -> ligar a back-reference e gravar o outro
                    if (!thisMultiple) {
                        // O:M inverso: o outro tem referência singular
                        writer.println("                " + otherVar + ".set" + capInv + "(" + entityVar + ");");
                    } else if (assoc.hasQualifier() && !thisIsMapEnd) {
                        // M:N inverso e o Map está do lado do OUTRO -> a back-reference é put por chave
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
        }

        // validar outra vez o estado final (já com as relações), invariantes que dependem de coleções
        // não eram vistas na validação inicial (corre antes de ligar). reaproveita a var 'errors'.
        if (anyLinked) {
            generateValidation(writer, cls.getName(), entityVar);
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

        // update all attributes from DTO; o atributo-id (se houver) é excluído:
        // a PK não muda em update, e para id auto-gerado o getter nem existe no CreateDTO
        String idAttrName = cls.getRoot().getIdAttributeName();
        List<Attribute> allAttrs = cls.getAllAttributes().stream()
                .filter(a -> !a.hasDerive())
                .filter(a -> !cls.isHiddenInUpdate(a.getName()))   // escondido do UpdateDTO -> não há getter, não se toca
                .filter(a -> !a.getName().equals(idAttrName))
                .toList();
        if (!allAttrs.isEmpty()) {
            writer.println("        // " + (fullReplace
                    ? "PUT full replace: escreve cada atributo tal e qual (omitido/null -> null)"
                    : "PATCH partial: null/omitido -> não mexe; valor -> aplica"));
            for (Attribute attr : allAttrs) {
                String getter = "get" + GeneratorUtils.capitalizeFirst(attr.getName());
                String setter = "set" + GeneratorUtils.capitalizeFirst(attr.getName());
                if (fullReplace) {
                    writer.println("        existing." + setter + "(dto." + getter + "());");
                } else {
                    writer.println("        if (dto." + getter + "() != null) existing." + setter + "(dto." + getter + "());");
                }
            }
            writer.println();
        }

        generateValidation(writer, cls.getName(), "existing");

        // liga só single refs por id; coleções não se tocam aqui (geridas pelos endpoints dedicados add/remove)
        generateApplyRelations(writer, cls, associations, "existing", false, fullReplace);

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
        if (entryMessage != null && entryMessage.getName() != null
                && !entryMessage.getName().equals(operName)) {
            throw new IllegalArgumentException(
                    "Diagrama da operação '" + operName + "': a mensagem de entrada '"
                    + entryMessage.getName() + "' não corresponde ao nome da operação.");
        }

        // tipo de retorno: da mensagem de entrada se especificado | senão da operação | senão void
        String returnType = (entryMessage != null && entryMessage.getReturnType() != null && !entryMessage.getReturnType().isBlank())
                ? GeneratorUtils.removePackageNames(entryMessage.getReturnType())
                : (op.getReturnType() != null ? GeneratorUtils.removePackageNames(op.getReturnType()) : "void");

        // assinatura a partir da mensagem de entrada; o tipo de cada argumento é obrigatório
        writer.println();
        writer.print("    public " + returnType + " " + operName + "(");
        if (entryMessage != null && !entryMessage.getArguments().isEmpty()) {
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
        // só carregamos self se alguma condição o referir (this.) ou houver snapshot @pre;
        // uma condição só sobre parâmetros (ex: pre: d > 0) não precisa de self nem do <entityVar>Id
        boolean needsSelf =
                op.getPreconditions().stream().anyMatch(p -> refsSelf(p.getJavaExpression()))
                || op.getPostconditions().stream().anyMatch(p -> refsSelf(p.getJavaExpression())
                        || !p.getPreSnapshots().isEmpty());

        if (needsSelf) {
            // self é carregado a partir de um parâmetro com o nome convencional <entityVar>Id (ex: turmaId)
            String idParam = selfVar + "Id";
            boolean hasIdParam = entryMessage != null && entryMessage.getArguments().stream()
                    .anyMatch(a -> idParam.equals(a.getName()));
            if (!hasIdParam) {
                throw new IllegalArgumentException("Operação '" + operName + "': condições que referem 'self' "
                        + "precisam de um parâmetro '" + idParam + "' para o carregar.");
            }
            // self é carregado pelo repositório da classe CONCRETA (a abstrata não tem repo),
            // mas tipado como a classe dona do diagrama (ex: Member member = studentMemberRepository...)
            String repoName = GeneratorUtils.lowercaseFirst(className) + "Repository";
            writer.println("        " + ownerName + " " + selfVar + " = " + repoName + ".findById(" + idParam + ")");
            writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className + " not found\"));");
            // pré-declara o self (carregado acima) para o SequenceBodyGenerator saltar o
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

        bodyGen.generateMethodBodyFromInteraction(writer, owner, op, interaction);   // lifeline do service = a do diagrama (owner)

        // a expressão de return vem da reply-message para o actor  sem reply não é feito return
        String returnExpr = GeneratorUtils.findReplyToActor(interaction.getElements());
        // OCL postcondition pode referir "result"
        // se for o caso, declarar variável "result" para o valor de return
        boolean needsResult = op.getPostconditions().stream()
                .anyMatch(p -> {
                    String je = p.getJavaExpression(selfVar);
                    return je != null && je.matches(".*\\bresult\\b.*");
                });
        // operação não-void precisa de reply; uma pós-condição que refere 'result' também
        if (!"void".equals(returnType) && returnExpr == null) {
            throw new IllegalArgumentException("Operação '" + operName + "': devolve '" + returnType
                    + "' mas o diagrama não tem mensagem de resposta (reply) para o ator.");
        }
        if (needsResult && returnExpr == null) {
            throw new IllegalArgumentException("Operação '" + operName
                    + "': uma pós-condição refere 'result' mas o diagrama não tem mensagem de resposta (reply).");
        }
        if (needsResult && "void".equals(returnType)) {
            throw new IllegalArgumentException("Operação '" + operName
                    + "': uma pós-condição refere 'result' mas a operação é void (não tem valor de retorno).");
        }
        if (needsResult && returnExpr != null && !"void".equals(returnType)) {
            writer.println("        " + returnType + " result = " + returnExpr + ";");
        }

        for (Postcondition post : op.getPostconditions()) {
            String javaExpr = post.getJavaExpression(selfVar);
            writer.println("        if (!(" + javaExpr + ")) {");
            writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(post.getMessage()) + "\");");
            writer.println("        }");
        }

        if (!"void".equals(returnType) && returnExpr != null) {
            writer.println("        return " + (needsResult ? "result" : returnExpr) + ";");
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

    // uma expressão Java traduzida de OCL refere self quando contém "this." (self -> this na tradução)
    private boolean refsSelf(String javaExpr) {
        return javaExpr != null && javaExpr.contains("this.");
    }

    // Escreve <expr>.checkInvariants() (que lança -> 400 via GlobalExceptionHandler) se a classe
    // 'className' tiver invariantes. Cada op valida TODA a entidade que persiste/muda, não só cls
    // (ex.: o outro lado de uma relação bidirecional, ou uma entidade gravada no applyRelations).
    private void generateValidation(PrintWriter writer, String className, String expr) {
        Class c = classMap.get(className);
        if (c == null || !c.hasAnyInvariants()) return;
        writer.println("        " + expr + ".checkInvariants();");
    }

    // idem, com indentação explícita — para os sítios aninhados (dentro de if/for no applyRelations)
    private void generateValidationTab(PrintWriter writer, String className, String expr, String ind) {
        Class c = classMap.get(className);
        if (c == null || !c.hasAnyInvariants()) return;
        writer.println(ind + expr + ".checkInvariants();");
    }

    // idem, mas o alvo pode ser null (ex.: dono anterior numa reatribuição, removeX sem relação)
    private void generateValidationIfPresent(PrintWriter writer, String className, String var) {
        Class c = classMap.get(className);
        if (c == null || !c.hasAnyInvariants()) return;
        writer.println("        if (" + var + " != null) " + var + ".checkInvariants();");
    }

}
