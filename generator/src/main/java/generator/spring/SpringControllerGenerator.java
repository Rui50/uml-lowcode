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

// id: @PathVariable/@RequestBody usam o tipo do id da própria classe e o das classes relacionadas (idTypeOf)
public class SpringControllerGenerator {
    private String outputDirectory;
    private String basePackage;

    // nome -> Class, para resolver o tipo de id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    public SpringControllerGenerator(String outputDirectory, String basePackage) {
        this.outputDirectory = outputDirectory;
        this.basePackage = basePackage;
    }

    // tipo do id de uma classe pelo nome
    private String idTypeOf(String className) {
        return GeneratorUtils.idType(classMap.get(className));
    }

    // gera todos os controladores
    public void generateControllers(List<Class> classes, List<Association> associations, List<Interaction> interactions) throws IOException {
        System.out.println("Generating Spring REST controllers...");

        String controllerDir = outputDirectory + "/src/main/java/" + basePackage.replace('.', '/') + "/controllers";
        new java.io.File(controllerDir).mkdirs();

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);

        for (Class cls : classes) {
            if (cls.isAbstract()) continue;
            if (!cls.isGenerateController()) continue;   // controller suprimido para esta classe
            generateController(cls, classes, associations, controllerDir, interactions);
        }

        generateExceptionHandler(controllerDir);
        generateNotFoundException();

        System.out.println("Generation of controllers completed.");
    }

    private void generateController(Class cls, List<Class> allClasses, List<Association> associations, String outputDir, List<Interaction> interactions) throws IOException {
        String className = cls.getName();
        String filename = outputDir + "/" + className + "Controller.java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ".controllers;");
            writer.println();

            generateImports(writer, className, cls, allClasses, associations);

            // class declaration
            writer.println("/**");
            writer.println(" * REST Controller for " + className);
            writer.println(" */");
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
            generateCustomOperationEndpoints(writer, cls, interactions, allClasses);

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

        writer.println("    // CRUD Endpoints");
        writer.println();

        // GET /api/entities - Get all
        writer.println("    /**");
        writer.println("     * GET /api/" + pluralPath);
        writer.println("     * Retrieve all " + className + " entities");
        writer.println("     * @return list of all entities");
        writer.println("     */");
        writer.println("    @GetMapping");
        writer.println("    public ResponseEntity<Page<" + className + "ResponseDTO>> getAll(Pageable pageable) {");
        writer.println("        return ResponseEntity.ok(" + serviceName + ".findAll(pageable));");
        writer.println("    }");
        writer.println();

        // GET /api/entities/{id} - Get by ID
        writer.println("    /**");
        writer.println("     * GET /api/" + pluralPath + "/{id}");
        writer.println("     * Retrieve " + className + " by ID");
        writer.println("     * @param id entity id");
        writer.println("     * @return entity if found, 404 if not");
        writer.println("     */");
        writer.println("    @GetMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> getById(@PathVariable " + idTypeOf(className) + " id) {");
        writer.println("        return " + serviceName + ".findById(id)");
        writer.println("            .map(ResponseEntity::ok)");
        writer.println("            .orElse(ResponseEntity.notFound().build());");
        writer.println("    }");
        writer.println();

        // POST /api/entities - Create (skip for abstract classes)
        if (!cls.isAbstract()) {
            writer.println("    /**");
            writer.println("     * POST /api/" + pluralPath);
            writer.println("     * Create new " + className);
            writer.println("     * @param dto entity data");
            writer.println("     * @return created entity");
            writer.println("     */");
            writer.println("    @PostMapping");
            writer.println("    public ResponseEntity<" + className + "ResponseDTO> create(@RequestBody " + className + "CreateDTO dto) {");
            writer.println("        " + className + "ResponseDTO created = " + serviceName + ".create(dto);");
            writer.println("        return ResponseEntity.status(HttpStatus.CREATED).body(created);");
            writer.println("    }");
            writer.println();
        }

        // PUT /api/entities/{id} - Update
        // PATCH (não PUT): a atualização é PARCIAL — campos/coleções não enviados ficam intactos.
        // PUT estrito (substituir tudo, omitido = limpar) exigiria presence-tracking (ver nota UpdateDTO).
        writer.println("    @PatchMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> update(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody " + className + "CreateDTO dto) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".update(id, dto);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();

        // DELETE /api/entities/{id} - Delete
        writer.println("    /**");
        writer.println("     * DELETE /api/" + pluralPath + "/{id}");
        writer.println("     * Delete " + className);
        writer.println("     * @param id entity id");
        writer.println("     * @return 204 No Content if deleted, 404 if not found");
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<Void> delete(@PathVariable " + idTypeOf(className) + " id) {");
        writer.println("        if (!" + serviceName + ".existsById(id)) {");
        writer.println("            return ResponseEntity.notFound().build();");
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

        for (Association assoc : associations) {
            // classe que DECLARA a associação (a própria ou um pai abstrato); null => não
            // participa, ou só a herda de pai concreto (que já gera os seus endpoints).
            String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;

            // numa auto associação bidirecional a classe é os dois lados, por isso processam-se
            // ambos — senão o lado DONO de um M:N self (onde isOwningSide só é true de um lado)
            // nunca geraria endpoint. Chama-se o helper uma vez por lado.
            boolean selfBidirectional = assoc.getSourceClass().equals(assoc.getTargetClass())
                    && assoc.isSourceNavigable() && assoc.isTargetNavigable();
            if (selfBidirectional) {
                RelationshipInfo source = new RelationshipInfo(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity(), assoc, true);
                RelationshipInfo target = new RelationshipInfo(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity(), assoc, false);
                hasRelationships = generateRelationEndpointsForSide(writer, className, serviceName, source, hasRelationships);
                hasRelationships = generateRelationEndpointsForSide(writer, className, serviceName, target, hasRelationships);
            } else {
                boolean isSource = assoc.getSourceClass().equals(declarer);
                RelationshipInfo rel = new RelationshipInfo(
                        isSource ? assoc.getTargetClass() : assoc.getSourceClass(),
                        isSource ? assoc.getTargetRole() : assoc.getSourceRole(),
                        isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity(),
                        assoc, isSource);
                hasRelationships = generateRelationEndpointsForSide(writer, className, serviceName, rel, hasRelationships);
            }
        }
    }

    // Gera os endpoints de relação para UMA perspetiva (lado) da associação.
    // Devolve o estado atualizado de hasRelationships
    private boolean generateRelationEndpointsForSide(PrintWriter writer, String className, String serviceName,
                                                     RelationshipInfo rel, boolean hasRelationships) {
        boolean thisMultiple = rel.thisMultiple();
        boolean otherMultiple = rel.otherMultiple();
        String otherClass = rel.otherClass;
        // nome do campo baseado no role (igual ao service/entidade); sem role usa o nome da classe
        String fieldName = rel.field();

        // ManyToOne: PUT /{id}/{other}/{otherId} e DELETE /{id}/{other}
        if (thisMultiple && !otherMultiple) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateAssignEndpoint(writer, className, otherClass, fieldName, serviceName);
            generateRemoveAssignEndpoint(writer, className, otherClass, fieldName, serviceName);
        }

        // ManyToMany: POST /{id}/{others} e DELETE /{id}/{others} — no lado DONO (isOwningSide)
        if (thisMultiple && otherMultiple && rel.isOwner()) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateCollectionAddEndpoint(writer, className, otherClass, fieldName, serviceName);
            generateCollectionRemoveEndpoint(writer, className, otherClass, fieldName, serviceName);
        }

        // OneToMany unidirectional (or qualifier/Map): POST /{id}/{others} e DELETE /{id}/{others}
        if ((!thisMultiple || rel.assoc.hasQualifier()) && (otherMultiple || rel.assoc.hasQualifier()) && !rel.isBidirectional()) {
            if (!hasRelationships) { writer.println("    // Relationship Endpoints"); hasRelationships = true; }
            generateCollectionAddEndpoint(writer, className, otherClass, fieldName, serviceName);
            generateCollectionRemoveEndpoint(writer, className, otherClass, fieldName, serviceName);
        }
        return hasRelationships;
    }

    /**
     * ManyToOne: PUT /{id}/{otherClass}/{otherId}
     * Ex: PUT /api/alunos/1/turno/3
     */
    private void generateAssignEndpoint(PrintWriter writer, String className, String otherClass, String fieldName, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String cap = GeneratorUtils.capitalizeFirst(fieldName);   // nome do método/handler (baseado no role)
        String otherPath = fieldName.toLowerCase();                // path baseado no role

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
    private void generateRemoveAssignEndpoint(PrintWriter writer, String className, String otherClass, String fieldName, String serviceName) {
        String cap = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPath = fieldName.toLowerCase();

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
    private void generateCollectionAddEndpoint(PrintWriter writer, String className, String otherClass, String fieldName, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String cap = GeneratorUtils.capitalizeFirst(fieldName);   // nome do método/handler (baseado no role)
        String otherPath = fieldName.toLowerCase();                // path baseado no role

        writer.println();
        writer.println("    /**");
        writer.println("     * Add " + otherClass + " entities to " + className + " by IDs");
        writer.println("     */");
        writer.println("    @PostMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> add" + cap + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".add" + cap + "(id, " + otherVar + "Ids);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany uni): DELETE /{id}/{others}
     * Ex: DELETE /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionRemoveEndpoint(PrintWriter writer, String className, String otherClass, String fieldName, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String cap = GeneratorUtils.capitalizeFirst(fieldName);
        String otherPath = fieldName.toLowerCase();

        writer.println("    /**");
        writer.println("     * Remove " + otherClass + " entities from " + className + " by IDs");
        writer.println("     * Ex: DELETE /{id}/" + otherPath + "?ids=1,2,3  (ids na query, não no body:");
        writer.println("     * um body em DELETE não tem semântica definida e pode ser ignorado por proxies/clientes)");
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
        writer.println("import " + basePackage + ".dto." + className + "CreateDTO;");
        writer.println("import " + basePackage + ".dto." + className + "ResponseDTO;");
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.http.HttpStatus;");
        writer.println("import org.springframework.http.ResponseEntity;");
        writer.println("import org.springframework.data.domain.Page;");
        writer.println("import org.springframework.data.domain.Pageable;");
        writer.println("import org.springframework.web.bind.annotation.*;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println("import java.util.UUID;");

        // jobs desta classe: importados para o endpoint de trigger manual
        for (Operation op : cls.getOperations()) {
            if (op.isJob()) writer.println("import " + basePackage + ".jobs."
                    + GeneratorUtils.capitalizeFirst(op.getName()) + "Job;");
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

        for (String extra : getExtraServices(cls, allClasses)) {
            writer.println("import " + basePackage + ".service." + extra + "Service;");
        }

        // ResponseDTOs (de outras entidades) + Collectors para endpoints custom que devolvem entidades
        Set<String> responseDtoImports = new HashSet<>();
        boolean needsCollectors = false;
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob() || op.getSequenceDiagram() == null) continue;
            Class owner = GeneratorUtils.declaringClassOf(cls, op);
            Message entry = SpringUtils.findEntryMessage(op.getSequenceDiagram(), owner, op);
            String rt = customOpReturnType(cls, op, entry);
            if ("void".equals(rt)) continue;
            String base = GeneratorUtils.extractBaseType(rt);
            if (classMap.containsKey(base)) {
                if (!base.equals(className)) responseDtoImports.add(base);
                if (rt.startsWith("List<")) needsCollectors = true;
            }
        }
        for (String e : responseDtoImports) {
            writer.println("import " + basePackage + ".dto." + e + "ResponseDTO;");
        }
        if (needsCollectors) {
            writer.println("import java.util.stream.Collectors;");
        }

        writer.println();
    }

    // entidades referenciadas em parametros de operações (excluindo a propria classe)
    private Set<String> getExtraServices(Class cls, List<Class> allClasses) {
        Set<String> extraServices = new HashSet<>();
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            for (Parameter param : op.getParameters()) {
                for (Class c : allClasses) {
                    if (c.getName().equals(param.getType()) && !c.getName().equals(cls.getName())) {
                        extraServices.add(c.getName());
                    }
                }
            }
        }
        return extraServices;
    }

    private void generateDependencies(PrintWriter writer, Class cls, List<Class> allClasses) {
        String className = cls.getName();
        writer.println("    private final " + className + "Service " +
                GeneratorUtils.lowercaseFirst(className) + "Service;");

        // para cada param de cada operação, se for uma entidade, adicionar como dependência
        for (String extra : getExtraServices(cls, allClasses)) {
            writer.println("    private final " + extra + "Service " + GeneratorUtils.lowercaseFirst(extra) + "Service;");
        }

        // jobs desta classe: injeta o @Component para o trigger manual
        for (Operation op : cls.getOperations()) {
            if (op.isJob()) {
                String jobClass = GeneratorUtils.capitalizeFirst(op.getName()) + "Job";
                writer.println("    private final " + jobClass + " " + GeneratorUtils.lowercaseFirst(jobClass) + ";");
            }
        }

        writer.println();
    }

    // exceção dedicada para "entidade não encontrada" — mapeada para 404 no GlobalExceptionHandler.
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

    private void generateExceptionHandler(String controllerDir) throws IOException {
        String filename = controllerDir + "/GlobalExceptionHandler.java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("package " + basePackage + ".controllers;");
            writer.println();
            writer.println("import org.springframework.http.HttpStatus;");
            writer.println("import org.springframework.http.ResponseEntity;");
            writer.println("import org.springframework.web.bind.annotation.ControllerAdvice;");
            writer.println("import org.springframework.web.bind.annotation.ExceptionHandler;");
            writer.println();
            writer.println("import java.util.Map;");
            writer.println("import jakarta.validation.ConstraintViolationException;");
            writer.println("import org.springframework.transaction.TransactionSystemException;");
            writer.println("import " + basePackage + ".exception.NotFoundException;");
            writer.println();
            writer.println("@ControllerAdvice");
            writer.println("public class GlobalExceptionHandler {");
            writer.println();
            writer.println("    @ExceptionHandler(IllegalArgumentException.class)");
            writer.println("    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException ex) {");
            writer.println("        return ResponseEntity.status(HttpStatus.BAD_REQUEST)");
            writer.println("            .body(Map.of(\"error\", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));");
            writer.println("    }");
            writer.println();
            writer.println("    // violações de pré/pós-condição (OCL) são erros do cliente -> 400, não 404");
            writer.println("    @ExceptionHandler(IllegalStateException.class)");
            writer.println("    public ResponseEntity<Map<String, String>> handleContractError(IllegalStateException ex) {");
            writer.println("        return ResponseEntity.status(HttpStatus.BAD_REQUEST)");
            writer.println("            .body(Map.of(\"error\", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));");
            writer.println("    }");
            writer.println();
            writer.println("    // violações Bean Validation (anotações OCL) -> 400. Vêm diretas (insert imediato");
            writer.println("    // com @GeneratedValue IDENTITY) OU embrulhadas em TransactionSystemException (commit/jobs)");
            writer.println("    @ExceptionHandler({ConstraintViolationException.class, TransactionSystemException.class})");
            writer.println("    public ResponseEntity<Map<String, String>> handleValidation(Exception ex) {");
            writer.println("        Throwable cause = (ex instanceof TransactionSystemException tse) ? tse.getRootCause() : ex;");
            writer.println("        if (cause instanceof ConstraintViolationException cve) {");
            writer.println("            String msg = cve.getConstraintViolations().stream()");
            writer.println("                .map(v -> v.getPropertyPath() + \" \" + v.getMessage())");
            writer.println("                .collect(java.util.stream.Collectors.joining(\", \"));");
            writer.println("            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(\"error\", msg));");
            writer.println("        }");
            writer.println("        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)");
            writer.println("            .body(Map.of(\"error\", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));");
            writer.println("    }");
            writer.println();
            writer.println("    // entidade não encontrada -> 404");
            writer.println("    @ExceptionHandler(NotFoundException.class)");
            writer.println("    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {");
            writer.println("        return ResponseEntity.status(HttpStatus.NOT_FOUND)");
            writer.println("            .body(Map.of(\"error\", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));");
            writer.println("    }");
            writer.println();
            writer.println("    // qualquer outro erro inesperado do servidor -> 500 (NÃO 404)");
            writer.println("    @ExceptionHandler(RuntimeException.class)");
            writer.println("    public ResponseEntity<Map<String, String>> handleServerError(RuntimeException ex) {");
            writer.println("        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)");
            writer.println("            .body(Map.of(\"error\", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));");
            writer.println("    }");
            writer.println("}");
        }

        System.out.println("Generated exception handler: " + filename);
    }

    /***
     * Diagramas sequencia
     */

    private void generateCustomOperationEndpoints(PrintWriter writer, Class cls, List<Interaction>
            interactions, List<Class> allClasses) {
        if (GeneratorUtils.effectiveOperations(cls).isEmpty()) return;

        String className = cls.getName();
        String serviceName = GeneratorUtils.lowercaseFirst(className) + "Service";

        writer.println();
        writer.println("    // Custom Operation Endpoints dos diagramas sequencia");

        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob()) continue;   // jobs são @Scheduled, não têm endpoint REST

            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;

            // diagrama foi desenhado na classe que declara a op; a chamada vai à do service concreto
            Class owner = GeneratorUtils.declaringClassOf(cls, op);
            Message entryMessage = SpringUtils.findEntryMessage(interaction, owner, op);
            List<MessageArgument> entryArgs = (entryMessage != null) ? entryMessage.getArguments() : new ArrayList<>();

            // endpoint parâmetros = mensagem de entrada; chama o service tal e qual
            // (a forma do request/response por DTO é uma anotação PSM futura — por agora @RequestParam)
            writer.println();
            String path = (op.getEndpointName() != null && !op.getEndpointName().isBlank())
                    ? op.getEndpointName() : op.getName();
            writer.println("    " + mappingAnnotation(op.getHttpMethod()) + "(\"/" + path + "\")");
            writer.print("    public ResponseEntity<?> " + op.getName() + "(");
            for (int i = 0; i < entryArgs.size(); i++) {
                if (i > 0) writer.print(",");
                MessageArgument arg = entryArgs.get(i);
                String argType = arg.getType() != null ? GeneratorUtils.removePackageNames(arg.getType()) : "Object";
                writer.print("\n            @RequestParam " + argType + " " + arg.getName());
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
                if (classMap.containsKey(base) && returnType.startsWith("List<")) {
                    // List<Entidade> -> mapeia cada um para o seu ResponseDTO
                    writer.println("        return ResponseEntity.ok(result.stream().map("
                            + base + "ResponseDTO::fromEntity).collect(Collectors.toList()));");
                } else if (classMap.containsKey(base)) {
                    // Entidade -> ResponseDTO
                    writer.println("        return ResponseEntity.ok(" + base + "ResponseDTO.fromEntity(result));");
                } else {
                    // primitivo / String / outro -> devolve diretamente
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
            String jobField = GeneratorUtils.lowercaseFirst(GeneratorUtils.capitalizeFirst(op.getName()) + "Job");
            String path = (op.getEndpointName() != null && !op.getEndpointName().isBlank())
                    ? op.getEndpointName() : op.getName();
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
            return GeneratorUtils.removePackageNames(entryMessage.getReturnType());
        }
        return op.getReturnType() != null ? GeneratorUtils.removePackageNames(op.getReturnType()) : "void";
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
