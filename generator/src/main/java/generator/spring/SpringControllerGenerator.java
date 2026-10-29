package generator.spring;

import generator.GeneratorUtils;
import generator.RelationshipInfo;
import model.*;
import model.Class;
import model.sequence.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gera os @RestController
 *
 * Endpoints:
 *  - CRUD: GET (list e /{id}), POST (create), PATCH /{id}, PUT /{id}, DELETE /{id};
 *  - ManyToOne: PUT /{id}/<attribute>/{otherId} + DELETE /{id}/<attribute>;
 *  - coleção: POST + DELETE /{id}/<attribute> (add/remove por ids);
 *  - ops custom (dos diagramas de sequência) e trigger manual dos jobs.
 *
 */

// id: @PathVariable/@RequestBody usam o tipo do id da própria classe e o das classes relacionadas
public class SpringControllerGenerator {
    private String outputDirectory;
    private String basePackage;

    // nome -> Class, para obter o tipo de id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    // nome de DTO -> classe onwer, para importar so folders de DTO que cada controller usa
    private final Map<String, String> DTO_ToClass;

    public SpringControllerGenerator(String outputDirectory, String basePackage, Map<String, String> DTO_ToClass) {
        this.outputDirectory = outputDirectory;
        this.basePackage = basePackage;
        this.DTO_ToClass = DTO_ToClass;
    }

    // tipo do id de uma classe pelo nome
    private String idTypeOf(String className) {
        return GeneratorUtils.idType(classMap.get(className));
    }

    // gera todos os controladores
    public void generateControllers(List<Class> classes, List<Association> associations) throws IOException {
        System.out.println("Generating Spring REST controllers...");

        String controllerDir = outputDirectory + "/src/main/java/" + basePackage.replace('.', '/') + "/controllers";
        new java.io.File(controllerDir).mkdirs();

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);

        for (Class cls : classes) {
            if (cls.isAbstract()) continue;
            if (!cls.isGenerateController()) continue;   // controller suprimido para esta classe
            generateController(cls, classes, associations, controllerDir);
        }

        generateExceptionHandler(controllerDir);
        generateNotFoundException();
        generateConflictException();

        System.out.println("Generation of controllers completed.");
    }

    private void generateController(Class cls, List<Class> allClasses, List<Association> associations, String outputDir) throws IOException {
        String className = cls.getName();
        String filename = outputDir + "/" + className + "Controller.java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ".controllers;");
            writer.println();

            generateImports(writer, className, cls, allClasses, associations);

            // class declaration
            writer.println("@RestController");

            String pluralPath = className.toLowerCase();
            if (!pluralPath.endsWith("s")){
                pluralPath = pluralPath + "s";
            }

            writer.println("@RequestMapping(\"/api/" + pluralPath + "\")");
            writer.println("@RequiredArgsConstructor");
            writer.println("public class " + className + "Controller {");
            writer.println();

            // dependencias (servicos)
            generateDependencies(writer, cls, allClasses);

            // CRUD endpoints
            generateCrudEndpoints(writer, cls);

            // endpoints para as relações
            generateRelationEndpoints(writer, cls, associations);

            // custom operation endpoints (sequence diag)
            generateCustomOperationEndpoints(writer, cls, allClasses);

            // trigger manual dos jobs desta classe
            generateJobTriggerEndpoints(writer, cls);

            writer.println("}");
        }

        System.out.println("Generated controller: " + filename);

    }
    private void generateCrudEndpoints(PrintWriter writer, Class cls) {
        String className = cls.getName();
        String varName = GeneratorUtils.lowercaseFirst(className);
        String serviceName = varName + "Service";
        String pluralPath = className.toLowerCase();
        if (!pluralPath.endsWith("s")){
            pluralPath = pluralPath + "s";
        }

        // GET /api/entities - Get all
        writer.println("    /**");
        writer.println("     * GET /api/" + pluralPath);
        writer.println("     * @return list of all entities");
        writer.println("     */");
        writer.println("    @GetMapping");
        writer.println("    public ResponseEntity<Page<" + SpringDTOGenerator.responseDTOName(cls, "List") + ">> getAll(Pageable pageable) {");
        writer.println("        return ResponseEntity.ok(" + serviceName + ".findAll(pageable));");
        writer.println("    }");
        writer.println();

        // GET /api/entities/{id} - Get by ID
        writer.println("    /**");
        writer.println("     * GET /api/" + pluralPath + "/{id}");
        writer.println("     * @param id entity id");
        writer.println("     * @return entity if found, 404 if not");
        writer.println("     */");
        writer.println("    @GetMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<" + SpringDTOGenerator.responseDTOName(cls, "Get") + "> getById(@PathVariable " + idTypeOf(className) + " id) {");
        writer.println("        " + SpringDTOGenerator.responseDTOName(cls, "Get") + " dto = " + serviceName + ".findById(id)");
        writer.println("            .orElseThrow(() -> new NotFoundException(\"" + className + " not found: \" + id));");
        writer.println("        return ResponseEntity.ok(dto);");
        writer.println("    }");
        writer.println();

        // POST /api/entities - Create
        // if (!cls.isAbstract())
        writer.println("    /**");
        writer.println("     * POST /api/" + pluralPath);
        writer.println("     * @param dto entity data");
        writer.println("     * @return created entity");
        writer.println("     */");
        writer.println("    @PostMapping");
        writer.println("    public ResponseEntity<" + SpringDTOGenerator.responseDTOName(cls, "Create") + "> create(@RequestBody " + className + "CreateDTO dto) {");
        writer.println("        " + SpringDTOGenerator.responseDTOName(cls, "Create") + " created = " + serviceName + ".create(dto);");
        writer.println("        return ResponseEntity.status(HttpStatus.CREATED).body(created);");
        writer.println("    }");
        writer.println();

        // atualização parcial em que campo null/omitido não mexe
        // (null não apaga: RFC-7386/JsonNullable não funciona em Boot 4/Jackson 3.)
        writer.println("    @PatchMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<" + SpringDTOGenerator.responseDTOName(cls, "Update") + "> update(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody " + className + "UpdateDTO dto) {");
        writer.println("        " + SpringDTOGenerator.responseDTOName(cls, "Update") + " updated = " + serviceName + ".update(id, dto);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();

        // PUT /api/entities/{id} faz full replace (not sent -> null), relations same as PATCH
        writer.println("    /**");
        writer.println("     * PUT /api/" + pluralPath + "/{id} — full replace so fields that arent sent are set to null.");
        writer.println("     */");
        writer.println("    @PutMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<" + SpringDTOGenerator.responseDTOName(cls, "Update") + "> replace(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody " + className + "UpdateDTO dto) {");
        writer.println("        " + SpringDTOGenerator.responseDTOName(cls, "Update") + " updated = " + serviceName + ".replace(id, dto);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();

        // DELETE /api/entities/{id} - Delete
        writer.println("    /**");
        writer.println("     * DELETE /api/" + pluralPath + "/{id}");
        writer.println("     * @param id entity id");
        writer.println("     * @return 204 No Content if deleted, 404 if not found");
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<Void> delete(@PathVariable " + idTypeOf(className) + " id) {");
        // o deleteById ignora um id inexistente em silêncio, lança-se como os outros 404 para ter o mesmo ProblemDetail
        writer.println("        if (!" + serviceName + ".existsById(id)) {");
        writer.println("            throw new NotFoundException(\"" + className + " not found: \" + id);");
        writer.println("        }");
        writer.println();
        writer.println("        " + serviceName + ".deleteById(id);");
        writer.println("        return ResponseEntity.noContent().build();");
        writer.println("    }");
        writer.println();
    }

    private void generateRelationEndpoints(PrintWriter writer, Class cls, List<Association> associations) {
        String className = cls.getName();
        String serviceName = GeneratorUtils.lowercaseFirst(className) + "Service";
        boolean hasRelationships = false;

        // collect() trata da orientação (declarer/herança + auto-assoc bidirecional)
        for (RelationshipInfo rel : RelationshipInfo.collect(cls, associations)) {
            hasRelationships = generateRelationEndpointsForSide(writer, className, serviceName, rel, hasRelationships);
        }
    }

    // Gera os endpoints de relação para um  lado da associação.
    private boolean generateRelationEndpointsForSide(PrintWriter writer, String className, String serviceName,
                                                     RelationshipInfo rel, boolean hasRelationships) {
        boolean inverseIsCollection = rel.inverseIsCollection();   // o outro lado tem muitos de mim
        boolean isCollection = rel.isCollection();                 // eu tenho muitos deles
        String otherClass = rel.otherClass;
        // nome do campo baseado no role
        String attributeName = rel.attributeName();

        // ManyToOne: PUT /{id}/{other}/{otherId} e DELETE /{id}/{other}
        if (inverseIsCollection && !isCollection) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateAssignEndpoint(writer, className, otherClass, attributeName, serviceName);
            generateRemoveAssignEndpoint(writer, className, otherClass, attributeName, serviceName);
        }
        // ManyToMany: POST /{id}/{others} e DELETE /{id}/{others}, no lado owner
        else if (inverseIsCollection && isCollection && rel.isOwner()) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateCollectionAddEndpoint(writer, className, otherClass, attributeName, serviceName);
            generateCollectionRemoveEndpoint(writer, className, otherClass, attributeName, serviceName);
        }
        // OneToMany unidirectional (or qualifier/Map): POST + DELETE /{id}/{others}
        else if (rel.hasCollectionEndpoint()) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateCollectionAddEndpoint(writer, className, otherClass, attributeName, serviceName);
            generateCollectionRemoveEndpoint(writer, className, otherClass, attributeName, serviceName);
        }
        // OneToOne: PUT /{id}/{other}/{otherId} e DELETE /{id}/{other}, como no ManyToOne
        else if (!inverseIsCollection && !isCollection) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateAssignEndpoint(writer, className, otherClass, attributeName, serviceName);
            generateRemoveAssignEndpoint(writer, className, otherClass, attributeName, serviceName);
        }
        return hasRelationships;
    }

    /**
     * ManyToOne: PUT /{id}/{otherClass}/{otherId}
     * Ex: PUT /api/alunos/1/turno/3
     */
    private void generateAssignEndpoint(PrintWriter writer, String className, String otherClass, String attributeName, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String cap = GeneratorUtils.capitalizeFirst(attributeName);   // nome do método/handler (baseado no role)
        String otherPath = attributeName.toLowerCase();                // path baseado no role

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne: Assign a " + otherClass + " to " + className);
        writer.println("     */");
        writer.println("    @PutMapping(\"/{id}/" + otherPath + "/{" + otherVar + "Id}\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> assign" + cap + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @PathVariable " + idTypeOf(otherClass) + " " + otherVar + "Id) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".assign" + cap + "(id, " + otherVar + "Id);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * ManyToOne: DELETE /{id}/{otherClass}
     * Ex: DELETE /api/alunos/1/turno
     */
    private void generateRemoveAssignEndpoint(PrintWriter writer, String className, String otherClass, String attributeName, String serviceName) {
        String cap = GeneratorUtils.capitalizeFirst(attributeName);
        String otherPath = attributeName.toLowerCase();

        writer.println("    /**");
        writer.println("     * ManyToOne: Remove the " + otherClass + " from " + className);
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> remove" + cap + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".remove" + cap + "(id);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany unidirectional): POST /{id}/{others}
     * Ex: POST /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionAddEndpoint(PrintWriter writer, String className, String otherClass, String attributeName, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String cap = GeneratorUtils.capitalizeFirst(attributeName);   // nome do método/handler (baseado no role)
        String otherPath = attributeName.toLowerCase();                // path baseado no role

        writer.println();
        writer.println("    /**");
        writer.println("     * Add " + otherClass + " entities to " + className + " by IDs");
        writer.println("     */");
        writer.println("    @PostMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> add" + cap + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody List<" + idTypeOf(otherClass) + "> ids) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".add" + cap + "(id, ids);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany uni): DELETE /{id}/{others}
     * Ex: DELETE /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionRemoveEndpoint(PrintWriter writer, String className, String otherClass, String attributeName, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String cap = GeneratorUtils.capitalizeFirst(attributeName);
        String otherPath = attributeName.toLowerCase();

        writer.println("    /**");
        writer.println("     * Remove " + otherClass + " entities from " + className + " by IDs");
        writer.println("     * DELETE /{id}/" + otherPath + "?ids=1,2,3  (ids na query, não no body");
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> remove" + cap + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestParam List<" + idTypeOf(otherClass) + "> ids) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".remove" + cap + "(id, ids);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    private void generateImports(PrintWriter writer, String className, Class cls,
                                 List<Class> allClasses, List<Association> associations) {

        writer.println("import " + basePackage + ".models." + className + ";");
        writer.println("import " + basePackage + ".service." + className + "Service;");
        writer.println("import " + basePackage + ".exception.NotFoundException;");
        // importa só as folders de DTO que este controller usa (a própria + as referidas nas ops custom)
        for (String dtoClass : SpringUtils.referencedDTOClasses(cls, classMap.keySet(), DTO_ToClass))
            writer.println("import " + basePackage + ".dto." + dtoClass.toLowerCase() + ".*;");
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.http.HttpStatus;");
        writer.println("import org.springframework.http.ResponseEntity;");
        writer.println("import org.springframework.data.domain.Page;");
        writer.println("import org.springframework.data.domain.Pageable;");
        writer.println("import org.springframework.web.bind.annotation.*;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println("import java.util.UUID;");
        writer.println("import java.time.*;");
        writer.println("import java.math.*;");

        // jobs desta classe: importados para o endpoint de trigger manual
        for (Operation op : cls.getOperations()) {
            if (op.isJob()) writer.println("import " + basePackage + ".jobs."
                    + cls.getName() + GeneratorUtils.capitalizeFirst(op.getName()) + "Job;");
        }

        // Imports de entidades usadas nas Operations
        List<String> modelsToImport = GeneratorUtils.getModelsToImportFromOperations(cls, allClasses);
        List<String> modelsToImportFromAssociations = GeneratorUtils.getModelsToImportFromAssociations(cls, associations);
        Set<String> allModelsToImport = new HashSet<>();
        allModelsToImport.addAll(modelsToImport);
        allModelsToImport.addAll(modelsToImportFromAssociations);

        if (!allModelsToImport.isEmpty()) {
            writer.println();
            for (String entityName : allModelsToImport) {
                writer.println("import " + basePackage + ".models." + entityName + ";");
            }
        }

        // Collectors para endpoints custom que devolvem List<entidade>
        boolean needsCollectors = false;
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob() || op.getSequenceDiagram() == null) continue;
            Class owner = GeneratorUtils.declaringClassOf(cls, op);
            Message entry = SpringUtils.findEntryMessage(op.getSequenceDiagram(), owner, op);
            String rt = customOpReturnType(cls, op, entry);
            if (rt.startsWith("List<") && classMap.containsKey(GeneratorUtils.extractBaseType(rt))) {
                needsCollectors = true;
            }
        }
        if (needsCollectors) {
            writer.println("import java.util.stream.Collectors;");
        }

        writer.println();
    }

    private void generateDependencies(PrintWriter writer, Class cls, List<Class> allClasses) {
        String className = cls.getName();
        writer.println("    private final " + className + "Service " +
                GeneratorUtils.lowercaseFirst(className) + "Service;");

        // jobs desta classe: injeta o @Component para o trigger manual
        for (Operation op : cls.getOperations()) {
            if (op.isJob()) {
                String jobClass = cls.getName() + GeneratorUtils.capitalizeFirst(op.getName()) + "Job";
                writer.println("    private final " + jobClass + " " + GeneratorUtils.lowercaseFirst(jobClass) + ";");
            }
        }

        writer.println();
    }

    // exceção dedicada para "entidade não encontrada" — mapeada para 404 no GlobalExceptionHandler.
    // https://medium.com/@butotoshalom/resourcenotfoundexception-in-spring-boot-what-why-and-how-efeaa93142e5
    private void generateNotFoundException() throws IOException {
        String exceptionDir = outputDirectory + "/src/main/java/" + basePackage.replace('.', '/') + "/exception";
        new java.io.File(exceptionDir).mkdirs();
        String filename = exceptionDir + "/NotFoundException.java";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("package " + basePackage + ".exception;");
            writer.println();
            writer.println("public class NotFoundException extends RuntimeException {");
            writer.println("    public NotFoundException(String message) {");
            writer.println("        super(message);");
            writer.println("    }");
            writer.println("}");
        }
        System.out.println("Generated exception: " + filename);
    }

    // exception  para "conflito de ligação" 409
    private void generateConflictException() throws IOException {
        String exceptionDir = outputDirectory + "/src/main/java/" + basePackage.replace('.', '/') + "/exception";
        new java.io.File(exceptionDir).mkdirs();
        String filename = exceptionDir + "/ConflictException.java";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("package " + basePackage + ".exception;");
            writer.println();
            writer.println("public class ConflictException extends RuntimeException {");
            writer.println("    public ConflictException(String message) {");
            writer.println("        super(message);");
            writer.println("    }");
            writer.println("}");
        }
        System.out.println("Generated exception: " + filename);
    }


    //https://www.geeksforgeeks.org/springboot/exception-handling-in-spring-boot/
    // https://dzone.com/articles/effective-exception-handling-java-spring-boot-application
    //https://stackoverflow.com/questions/49177111/production-level-exception-handling-in-spring-boot
    //https://blog.anilgulati.com/mastering-exception-handling-in-spring-boot
    private void generateExceptionHandler(String controllerDir) throws IOException {
        String filename = controllerDir + "/GlobalExceptionHandler.java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("package " + basePackage + ".controllers;");
            writer.println();
            writer.println("import org.springframework.http.HttpStatus;");
            writer.println("import org.springframework.http.ProblemDetail;");
            writer.println("import org.springframework.web.bind.annotation.ControllerAdvice;");
            writer.println("import org.springframework.web.bind.annotation.ExceptionHandler;");
            writer.println();
            writer.println("import jakarta.validation.ConstraintViolationException;");
            writer.println("import org.springframework.transaction.TransactionSystemException;");
            writer.println("import org.springframework.dao.DataIntegrityViolationException;");
            writer.println("import " + basePackage + ".exception.NotFoundException;");
            writer.println("import " + basePackage + ".exception.ConflictException;");
            writer.println("import org.slf4j.Logger;");
            writer.println("import org.slf4j.LoggerFactory;");
            writer.println();
            writer.println("// Respostas de erro em formato ProblemDetail");
            writer.println("// https://www.baeldung.com/spring-boot-return-errors-problemdetail");
            writer.println("@ControllerAdvice");
            writer.println("public class GlobalExceptionHandler {");
            writer.println();
            writer.println("    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);");
            writer.println();
            writer.println("    @ExceptionHandler({");
            writer.println("        IllegalArgumentException.class,     // OCL: invariante falhou (checkInvariants)");
            writer.println("        IllegalStateException.class,        // OCL: pré ou pós-condição falhou");
            writer.println("        ConstraintViolationException.class, // Bean Validation (@NotNull, @Size...) ");
            writer.println("        TransactionSystemException.class    // err no commit ");
            writer.println("    })");
            writer.println("    public ProblemDetail handleValidation(Exception ex) {");
            writer.println("        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg(ex));");
            writer.println("    }");
            writer.println();
            // BD recusa (FK ainda referenciada, ou valor único duplicado) -> 409
            writer.println("    @ExceptionHandler(DataIntegrityViolationException.class)");
            writer.println("    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {");
            writer.println("        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,");
            writer.println("            \"Data integrity violation: the resource is still referenced by other records or violates a constraint.\");");
            writer.println("    }");
            writer.println();
            writer.println("    // entidade não encontrada -> 404");
            writer.println("    @ExceptionHandler(NotFoundException.class)");
            writer.println("    public ProblemDetail handleNotFound(NotFoundException ex) {");
            writer.println("        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, msg(ex));");
            writer.println("    }");
            writer.println();
            writer.println("    // conflito de ligação -> 409");
            writer.println("    @ExceptionHandler(ConflictException.class)");
            writer.println("    public ProblemDetail handleConflict(ConflictException ex) {");
            writer.println("        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, msg(ex));");
            writer.println("    }");
            writer.println();
            writer.println("    // qualquer outro erro -> 500");
            writer.println("    @ExceptionHandler(RuntimeException.class)");
            writer.println("    public ProblemDetail handleServerError(RuntimeException ex) {");
            writer.println("        log.error(\"Unhandled server error\", ex);");
            writer.println("        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, \"Internal server error\");");
            writer.println("    }");
            writer.println();
            writer.println("    // mensagem do erro, ou o nome da exceção se não houver mensagem");
            writer.println("    private static String msg(Throwable ex) {");
            writer.println("        if (ex.getMessage() != null) {");
            writer.println("            return ex.getMessage();");
            writer.println("        } else {");
            writer.println("            return ex.getClass().getSimpleName();");
            writer.println("        }");
            writer.println("    }");
            writer.println("}");
        }

        System.out.println("Generated exception handler: " + filename);
    }

    /***
     * Diagramas sequencia
     */

    private void generateCustomOperationEndpoints(PrintWriter writer, Class cls, List<Class> allClasses) {
        if (GeneratorUtils.effectiveOperations(cls).isEmpty()) return;

        String className = cls.getName();
        String serviceName = GeneratorUtils.lowercaseFirst(className) + "Service";

        writer.println();
        writer.println("    // Custom Operation Endpoints");

        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob()) continue;   // jobs são @Scheduled, não têm endpoint REST

            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;

            // diagrama foi desenhado na classe que declara a op, a chamada vai à do service
            Class owner = GeneratorUtils.declaringClassOf(cls, op);
            Message entryMessage = SpringUtils.findEntryMessage(interaction, owner, op);
            List<MessageArgument> entryArgs = (entryMessage != null) ? entryMessage.getArguments() : new ArrayList<>();

            // parâmetros do endpoint = args da mensagem de entrada, o endpoint so chama o service com eles
            // binding por tipo do arg       DTO -> @RequestBody, simples -> @RequestParam

            String path;
            if (op.getEndpointName() != null && !op.getEndpointName().isBlank()) {
                path = op.getEndpointName();
            } else {
                path = op.getName();
            }

            writer.println("    " + mappingAnnotation(op.getHttpMethod()) + "(\"/" + path + "\")");

            writer.print("    public ResponseEntity<?> " + op.getName() + "(");
            for (int i = 0; i < entryArgs.size(); i++) {
                if (i > 0) writer.print(",");
                MessageArgument arg = entryArgs.get(i);
                String binding;
                if (GeneratorUtils.isSimpleParamType(arg.getType())) {
                    binding = "@RequestParam";   // primitivo / String / data
                } else {
                    // assume-se um DTO
                    // futuro
                    // lançar erro claro se o DTO não existir, em vez de deixar falhar na compilação da app gerada.
                    binding = "@RequestBody";
                }
                writer.print("\n            " + binding + " " + arg.getType() + " " + arg.getName());
            }
            writer.println(") {");

            StringBuilder args = new StringBuilder();
            for (int i = 0; i < entryArgs.size(); i++) {
                if (i > 0) args.append(", ");
                args.append(entryArgs.get(i).getName());
            }

            // tipo de retorno do service: mensagem de entrada ":Type" | senão operação | senão void
            String returnType = customOpReturnType(cls, op, entryMessage);
            String call = serviceName + "." + op.getName() + "(" + args + ")";
            if ("void".equals(returnType)) {
                writer.println("        " + call + ";");
                writer.println("        return ResponseEntity.ok().build();");
            } else {
                String base = GeneratorUtils.extractBaseType(returnType);
                writer.println("        " + returnType + " result = " + call + ";");
                // List<Entidade> -> mapeia cada um para o seu ResponseDTO
                if (classMap.containsKey(base) && returnType.startsWith("List<")) {
                    writer.println("        return ResponseEntity.ok(result.stream().map("
                            + base + "ResponseDTO::fromEntity).collect(Collectors.toList()));");
                }
                // Entidade -> ResponseDTO
                else if (classMap.containsKey(base)) {
                    writer.println("        return ResponseEntity.ok(" + base + "ResponseDTO.fromEntity(result));");
                }
                // primitivo / String / outro -> devolve diretamente
                else {
                    writer.println("        return ResponseEntity.ok(result);");
                }
            }
            writer.println("    }");
        }
    }

    // trigger manual de cada job desta classe: POST /api/<plural>/<op> -> job.run().
    // Os jobs correm @Scheduled na cron, mas assim também se disparam à mão (útil em demos).
    private void generateJobTriggerEndpoints(PrintWriter writer, Class cls) {
        for (Operation op : cls.getOperations()) {
            if (!op.isJob()) continue;
            String jobField = GeneratorUtils.lowercaseFirst(cls.getName() + GeneratorUtils.capitalizeFirst(op.getName()) + "Job");
            String path;
            if (op.getEndpointName() != null && !op.getEndpointName().isBlank()) {
                path = op.getEndpointName();
            } else {
                path = op.getName();
            }
            writer.println();
            writer.println("    @PostMapping(\"/" + path + "\")");
            writer.println("    public ResponseEntity<?> " + op.getName() + "() {");
            writer.println("        " + jobField + ".run();");
            writer.println("        return ResponseEntity.ok().build();");
            writer.println("    }");
        }
    }

    // tipo de retorno do endpoint custom: mensagem de entrada ":Type" | senão operação | senão void
    private String customOpReturnType(Class cls, Operation op, Message entryMessage) {
        if (entryMessage != null && entryMessage.getReturnType() != null && !entryMessage.getReturnType().isBlank()) {
            return entryMessage.getReturnType();
        }
        return op.getReturnType() != null ? op.getReturnType() : "void";
    }

    //  HTTP -> anotação de mapping do Spring, o post fica como default
    private String mappingAnnotation(String httpMethod) {
        String method = (httpMethod == null || httpMethod.isBlank()) ? "POST" : httpMethod.trim().toUpperCase();
        return switch (method) {
            case "GET"    -> "@GetMapping";
            case "PUT"    -> "@PutMapping";
            case "DELETE" -> "@DeleteMapping";
            case "PATCH"  -> "@PatchMapping";
            default       -> "@PostMapping";
        };
    }
}
