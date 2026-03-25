package generator.spring;

import model.*;
import model.Class;
import model.sequence.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpringControllerGenerator {
    private String outputDirectory;
    private String basePackage = "com.example.app";

    public SpringControllerGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // gera todos os controladores
    public void generateControllers(List<Class> classes, List<Association> associations, List<Interaction> interactions) throws IOException {
        System.out.println("Generating Spring REST controllers...");

        String controllerDir = outputDirectory + "/src/main/java/com/example/app/controllers";
        new java.io.File(controllerDir).mkdirs();

        for (Class cls : classes) {
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
            //generateCustomOperationEndpoints(writer, cls, interactions, allClasses);

            writer.println("}");
        }

        System.out.println("Generated controller: " + filename);

    }
    private void generateCrudEndpoints(PrintWriter writer, Class cls) {
        String className = cls.getName();
        String varName = toLowerCaseFirst(className);
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
        writer.println("    public ResponseEntity<List<" + className + ">> getAll() {");
        writer.println("        List<" + className + "> entities = " + serviceName + ".findAll();");
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
        writer.println("    public ResponseEntity<" + className + "> getById(@PathVariable Long id) {");
        writer.println("        return " + serviceName + ".findById(id)");
        writer.println("            .map(ResponseEntity::ok)");
        writer.println("            .orElse(ResponseEntity.notFound().build());");
        writer.println("    }");
        writer.println();

        // POST /api/entities - Create
        writer.println("    /**");
        writer.println("     * POST /api/" + pluralPath);
        writer.println("     * Create new " + className);
        writer.println("     * @param " + varName + " entity data");
        writer.println("     * @return created entity");
        writer.println("     */");
        writer.println("    @PostMapping");
        writer.println("    public ResponseEntity<" + className + "> create(@RequestBody " + className + " " + varName + ") {");
        writer.println("        " + className + " created = " + serviceName + ".create(" + varName + ");");
        writer.println("        return ResponseEntity.status(HttpStatus.CREATED).body(created);");
        writer.println("    }");
        writer.println();

        // PUT /api/entities/{id} - Update
        writer.println("    @PutMapping(\"/{id}\")");
        writer.println("    public ResponseEntity<" + className + "> update(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @RequestBody " + className + " " + varName + ") {");
        writer.println("        " + className + " updated = " + serviceName + ".update(id, " + varName + ");");
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
        writer.println("    public ResponseEntity<Void> delete(@PathVariable Long id) {");
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
        String varName = toLowerCaseFirst(className);
        String serviceName = varName + "Service";
        boolean hasRelationships = false;

        for (Association assoc : associations) {
            boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();

            boolean isSource = assoc.getSourceClass().equals(className);
            boolean isTarget = assoc.getTargetClass().equals(className);
            if (!isSource && !isTarget) continue;

            String thisMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String otherMul = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
            boolean thisMultiple = checkMultiplicity(thisMul);
            boolean otherMultiple = checkMultiplicity(otherMul);

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
        String otherVar = toLowerCaseFirst(otherClass);
        String otherPath = otherVar.toLowerCase();

        writer.println();
        writer.println("    /**");
        writer.println("     * ManyToOne: Assign a " + otherClass + " to " + className);
        writer.println("     */");
        writer.println("    @PutMapping(\"/{id}/" + otherPath + "/{" + otherVar + "Id}\")");
        writer.println("    public ResponseEntity<" + className + "> assign" + otherClass + "(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @PathVariable Long " + otherVar + "Id) {");
        writer.println("        " + className + " updated = " + serviceName + ".assign" + otherClass + "(id, " + otherVar + "Id);");
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
        writer.println("    public ResponseEntity<" + className + "> remove" + otherClass + "(");
        writer.println("            @PathVariable Long id) {");
        writer.println("        " + className + " updated = " + serviceName + ".remove" + otherClass + "(id);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany unidirectional): POST /{id}/{others}
     * Ex: POST /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionAddEndpoint(PrintWriter writer, String className, String otherClass, String serviceName) {
        String otherVar = toLowerCaseFirst(otherClass);
        String otherPlural = otherVar + "s";
        String otherPath = otherPlural.toLowerCase();

        writer.println();
        writer.println("    /**");
        writer.println("     * Add " + otherClass + " entities to " + className + " by IDs");
        writer.println("     */");
        writer.println("    @PostMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "> add" + capitalizeFirstLetter(otherPlural) + "(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @RequestBody List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " updated = " + serviceName + ".add" + capitalizeFirstLetter(otherPlural) + "(id, " + otherVar + "Ids);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    /**
     * Collection (ManyToMany / OneToMany uni): DELETE /{id}/{others}
     * Ex: DELETE /api/turmas/1/alunos  body: [1, 2, 3]
     */
    private void generateCollectionRemoveEndpoint(PrintWriter writer, String className, String otherClass, String serviceName) {
        String otherVar = toLowerCaseFirst(otherClass);
        String otherPlural = otherVar + "s";
        String otherPath = otherPlural.toLowerCase();

        writer.println("    /**");
        writer.println("     * Remove " + otherClass + " entities from " + className + " by IDs");
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}/" + otherPath + "\")");
        writer.println("    public ResponseEntity<" + className + "> remove" + capitalizeFirstLetter(otherPlural) + "(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @RequestBody List<Long> " + otherVar + "Ids) {");
        writer.println("        " + className + " updated = " + serviceName + ".remove" + capitalizeFirstLetter(otherPlural) + "(id, " + otherVar + "Ids);");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();
    }

    private void generateImports(PrintWriter writer, String className, Class cls,
                                 List<Class> allClasses, List<Association> associations) {

        writer.println("import " + basePackage + ".models." + className + ";");
        writer.println("import " + basePackage + ".service." + className + "Service;");
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.http.HttpStatus;");
        writer.println("import org.springframework.http.ResponseEntity;");
        writer.println("import org.springframework.web.bind.annotation.*;");
        writer.println();
        //writer.println("import jakarta.validation.Valid;");
        writer.println("import java.util.List;");

        // Imports de entidades usadas nas Operations
        List<String> modelsToImport = getModelsToImportFromOperations(cls, allClasses);
        List<String> modelsToImportFromAssociations = getModelsToImportFromAssociations(cls, associations);
        Set<String> allModelsToImport = new HashSet<>();
        allModelsToImport.addAll(modelsToImport);
        allModelsToImport.addAll(modelsToImportFromAssociations);

        if (!allModelsToImport.isEmpty()) {
            writer.println();
            for (String entityName : allModelsToImport) {
                writer.println("import " + basePackage + ".models." + entityName + ";");
            }
        }

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
        for (String extra : extraServices) {
            writer.println("import " + basePackage + ".service." + extra + "Service;");
        }

        writer.println();
    }

    private void generateDependencies(PrintWriter writer, Class cls, List<Class> allClasses) {
        String className = cls.getName();
        writer.println("    private final " + className + "Service " +
                toLowerCaseFirst(className) + "Service;");

        // para cada param de cada operação, se for uma entidade, adicionar como dependência
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
        for (String extra : extraServices) {
            writer.println("    private final " + extra + "Service " + toLowerCaseFirst(extra) + "Service;");
        }

        writer.println();
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

    // extrair para um utils depois
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

    private String removePackageNames(String type) {
        if (type == null) return null;

        //List<Teste.Aluno> -> List<Aluno>

        return type.replaceAll("\\b([A-Za-z_$][\\w$]*\\.)+([A-Za-z_$][\\w$]*)\\b", "$2");
    }

    private String toLowerCaseFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private boolean checkMultiplicity(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
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
            writer.println("            .body(Map.of(\"error\", ex.getMessage()));");
            writer.println("    }");
            writer.println();
            writer.println("    @ExceptionHandler(RuntimeException.class)");
            writer.println("    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {");
            writer.println("        return ResponseEntity.status(HttpStatus.NOT_FOUND)");
            writer.println("            .body(Map.of(\"error\", ex.getMessage()));");
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
        String serviceName = toLowerCaseFirst(className) + "Service";

        writer.println();
        writer.println("    // Custom Operation Endpoints dos diagramas sequencia");

        for (Operation op : cls.getOperations()) {
            Interaction interaction = findInteractionForOperation(cls, op, interactions, allClasses);

            writer.println();

            // method sign and @Post for now
            writer.println("    @PostMapping(\"/{id}/" + op.getName() + "\")");
            writer.print("    public ResponseEntity<?> " + op.getName() + "(\n");
            writer.print("            @PathVariable Long id");

            // para cada param: tipos -> @RequestParam Long paramNameId
            //                  normais -> @RequestParam tipo paramName

            for (Parameter param : op.getParameters()) {
                String paramType = removePackageNames(param.getType());
                boolean isEntity = false;
                for (Class c : allClasses) {
                    if (c.getName().equals(paramType)) {
                        isEntity = true;
                        break;
                    }
                }

                if (isEntity) {
                    writer.print(",\n            @RequestParam Long " + param.getName() + "Id");
                } else {
                    writer.print(",\n            @RequestParam " + paramType + " " + param.getName());
                }
            }
            writer.println(") {");

            // get entidades

            for (Parameter param : op.getParameters()) {
                String paramType = removePackageNames(param.getType());
                boolean isEntity = false;
                for (Class c : allClasses) {
                    if (c.getName().equals(paramType)) {
                        isEntity = true;
                        break;
                    }
                }

                if (isEntity) {
                    String paramServiceName = toLowerCaseFirst(paramType) + "Service";
                    writer.println("        " + paramType + " " + param.getName() + " = " + paramServiceName
                            + ".findById(" + param.getName() + "Id)");
                    writer.println("            .orElseThrow(() -> new RuntimeException(\"" + paramType + " " +
                            "not found: \" + " + param.getName() + "Id));");
                }
            }

            // chamar o serviço
            writer.println();
            StringBuilder args = new StringBuilder("id");
            for (Parameter param : op.getParameters()) {
                args.append(", ").append(param.getName());
            }

            String returnType = op.getReturnType();
            if (returnType != null && !"void".equals(returnType)) {
                writer.println("        " + returnType + " result = " + serviceName + "." + op.getName() +
                        "(" + args + ");");
                writer.println("        return ResponseEntity.ok(result);");
            } else {
                writer.println("        " + serviceName + "." + op.getName() + "(" + args + ");");
                writer.println("        return ResponseEntity.ok().build();");
            }

            writer.println("    }");

        }
    }
    // Encontrar o diagrama correspondente a uma operaão
    // Fica assim para ja, depois para ter mais organizado ou associamos dentro da ferramenta ou msm ao fazer o diagrama no nome
    private Interaction findInteractionForOperation(Class cls, Operation op, List<Interaction>
            interactions, List<Class> allClasses) {

        // Pegar nos nomes das classes
        Set<String> classNames = allClasses.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        for (Interaction interaction : interactions) {
            for (Message msg : interaction.getMessages()) {
                // ponto de entrada pode ser o nome da operação ou o nome do diagrama de sequencia
                //if(!"synchCall".equals(msg.getMessageSort())) continue; // só  interessam mensagens sync
                if(msg.getSource() == null || msg.getTarget() == null) continue;

                String sourceType = msg.getSource().getType();
                if(classNames.contains(sourceType)) continue;

                if(msg.getName().equals(op.getName()) && msg.getTarget().getType().equals(cls.getName())) return interaction;
                break;
            }
        }

        return null;
    }

}
