package generator.spring;

import generator.GeneratorUtils;
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
        Class c = classMap.get(className);
        return (c != null) ? c.getRoot().getIdAttributeType() : "Long";
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
        writer.println("    public ResponseEntity<List<" + className + "ResponseDTO>> getAll() {");
        writer.println("        List<" + className + "ResponseDTO> entities = " + serviceName + ".findAll();");
        writer.println("        return ResponseEntity.ok(entities);");
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
        writer.println("    @PutMapping(\"/{id}\")");
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
        String varName = GeneratorUtils.lowercaseFirst(className);
        String serviceName = varName + "Service";
        boolean hasRelationships = false;

        for (Association assoc : associations) {
            boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

            // matching local — associações herdadas tratadas pelo controller do pai (polimórfico)
            boolean isSource = assoc.getSourceClass().equals(className);
            boolean isTarget = assoc.getTargetClass().equals(className);
            if (!isSource && !isTarget) continue;

            String thisMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherMul = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
            boolean thisMultiple = GeneratorUtils.checkMultiplicity(thisMul);
            boolean otherMultiple = GeneratorUtils.checkMultiplicity(otherMul);

            boolean navigableToOther = isSource ? assoc.isTargetNavigable() : assoc.isSourceNavigable();
            if (!navigableToOther) continue;

            String otherClass = isSource ? assoc.getTargetClass() : assoc.getSourceClass();

            // ManyToOne: PUT /{id}/{other}/{otherId} e DELETE /{id}/{other}
            if (thisMultiple && !otherMultiple) {
                if (!hasRelationships) {
                    writer.println("    // Relationship Endpoints");
                    hasRelationships = true;
                }
                generateAssignEndpoint(writer, className, otherClass, serviceName);
                generateRemoveAssignEndpoint(writer, className, otherClass, serviceName);
            }

            // ManyToMany: POST /{id}/{others} e DELETE /{id}/{others}
            if (thisMultiple && otherMultiple && isSource) {
                if (!hasRelationships) {
                    writer.println("    // Relationship Endpoints");
                    hasRelationships = true;
                }
                generateCollectionAddEndpoint(writer, className, otherClass, serviceName);
                generateCollectionRemoveEndpoint(writer, className, otherClass, serviceName);
            }

            // OneToMany unidirectional (or qualifier/Map): POST /{id}/{others} e DELETE /{id}/{others}
            if ((!thisMultiple || assoc.hasQualifier()) && (otherMultiple || assoc.hasQualifier()) && !isBidirectional) {
                if (!hasRelationships) {
                    writer.println("    // Relationship Endpoints");
                    hasRelationships = true;
                }
                generateCollectionAddEndpoint(writer, className, otherClass, serviceName);
                generateCollectionRemoveEndpoint(writer, className, otherClass, serviceName);
            }
        }
    }

    /**
     * ManyToOne: PUT /{id}/{otherClass}/{otherId}
     * Ex: PUT /api/alunos/1/turno/3
     */
    private void generateAssignEndpoint(PrintWriter writer, String className, String otherClass, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherPath = otherVar.toLowerCase();

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne: Assign a " + otherClass + " to " + className);
        writer.println("     */");
        writer.println("    @PutMapping(\"/{id}/" + otherPath + "/{" + otherVar + "Id}\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> assign" + otherClass + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @PathVariable " + idTypeOf(otherClass) + " " + otherVar + "Id) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".assign" + otherClass + "(id, " + otherVar + "Id);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * ManyToOne: DELETE /{id}/{otherClass}
     * Ex: DELETE /api/alunos/1/turno
     */
    private void generateRemoveAssignEndpoint(PrintWriter writer, String className, String otherClass, String serviceName) {
        String otherPath = otherClass.toLowerCase();

        writer.println("    /**");
        writer.println("     * ManyToOne: Remove the " + otherClass + " from " + className);
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> remove" + otherClass + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".remove" + otherClass + "(id);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany unidirectional): POST /{id}/{others}
     * Ex: POST /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionAddEndpoint(PrintWriter writer, String className, String otherClass, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherPlural = otherVar + "s";
        String otherPath = otherPlural.toLowerCase();

        writer.println();
        writer.println("    /**");
        writer.println("     * Add " + otherClass + " entities to " + className + " by IDs");
        writer.println("     */");
        writer.println("    @PostMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> add" + GeneratorUtils.capitalizeFirst(otherPlural) + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".add" + GeneratorUtils.capitalizeFirst(otherPlural) + "(id, " + otherVar + "Ids);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany uni): DELETE /{id}/{others}
     * Ex: DELETE /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionRemoveEndpoint(PrintWriter writer, String className, String otherClass, String serviceName) {
        String otherVar = GeneratorUtils.lowercaseFirst(otherClass);
        String otherPlural = otherVar + "s";
        String otherPath = otherPlural.toLowerCase();

        writer.println("    /**");
        writer.println("     * Remove " + otherClass + " entities from " + className + " by IDs");
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "ResponseDTO> remove" + GeneratorUtils.capitalizeFirst(otherPlural) + "(");
        writer.println("            @PathVariable " + idTypeOf(className) + " id,");
        writer.println("            @RequestBody List<" + idTypeOf(otherClass) + "> " + otherVar + "Ids) {");
        writer.println("        " + className + "ResponseDTO updated = " + serviceName + ".remove" + GeneratorUtils.capitalizeFirst(otherPlural) + "(id, " + otherVar + "Ids);");
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
        writer.println("import org.springframework.web.bind.annotation.*;");
        writer.println();
        writer.println("import java.util.List;");

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

        writer.println();
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
            writer.println("    @ExceptionHandler(RuntimeException.class)");
            writer.println("    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {");
            writer.println("        return ResponseEntity.status(HttpStatus.NOT_FOUND)");
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
