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

        writer.println();
    }

    // entidades referenciadas em parametros de operações (excluindo a propria classe)
    private Set<String> getExtraServices(Class cls, List<Class> allClasses) {
        Set<String> extraServices = new HashSet<>();
        for (Operation op : cls.getOperations()) {
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
        if (cls.getOperations().isEmpty()) return;

        String className = cls.getName();
        String serviceName = GeneratorUtils.lowercaseFirst(className) + "Service";

        writer.println();
        writer.println("    // Custom Operation Endpoints dos diagramas sequencia");

        for (Operation op : cls.getOperations()) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;

            Message entryMessage = SpringUtils.findEntryMessage(interaction, cls, op);
            List<MessageArgument> entryArgs = (entryMessage != null) ? entryMessage.getArguments() : new ArrayList<>();

            // endpoint parâmetros = mensagem de entrada; chama o service tal e qual
            // (a forma do request/response por DTO é uma anotação PSM futura — por agora @RequestParam)
            writer.println();
            writer.println("    @PostMapping(\"/" + op.getName() + "\")");
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
            writer.println("        " + serviceName + "." + op.getName() + "(" + args + ");");
            writer.println("        return ResponseEntity.ok().build();");
            writer.println("    }");
        }
    }
}
