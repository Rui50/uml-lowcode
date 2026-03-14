package generator.spring;

import model.*;
import model.Class;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpringControllerGenerator {
    private String outputDirectory;
    private String basePackage = "com.example.app";

    public SpringControllerGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // gera todos os controladores
    public void generateControllers(List<Class> classes, List<Association> associations) throws IOException {
        System.out.println("Generating Spring REST controllers...");

        String controllerDir = outputDirectory + "/src/main/java/com/example/app/controllers";
        new java.io.File(controllerDir).mkdirs();

        for (Class cls : classes) {
            generateController(cls, classes, associations, controllerDir);
        }

        System.out.println("Generation of controllers completed.");
    }

    private void generateController(Class cls, List<Class> allClasses, List<Association> associations, String outputDir) throws IOException {
        String className = cls.getName();
        String filename = outputDir + "/" + className + "Controller.java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ".controller;");
            writer.println();

            generateImports(writer, className, cls, allClasses, associations);

            // class declaration
            writer.println("/**");
            writer.println(" * REST Controller for " + className);
            writer.println(" */");
            writer.println("@RestController");

            String pluralName = className;
            if (!className.endsWith("s")){
                pluralName = className + "s";
            }

            writer.println("@RequestMapping(\"/api/" + pluralName + "\")");
            writer.println("@RequiredArgsConstructor");
            writer.println("public class " + className + "Controller {");
            writer.println();

            // dependencias (servicos)
            generateDependencies(writer, className);

            // CRUD endpoints
            generateCrudEndpoints(writer, cls);

            // endpoints para as relações
            generateRelationEndpoints(writer, cls, associations);

            writer.println("}");
        }

        System.out.println("Generated controller: " + filename);

    }
    private void generateCrudEndpoints(PrintWriter writer, Class cls) {
        String className = cls.getName();
        String varName = toLowerCaseFirst(className);
        String serviceName = varName + "Service";
        String pluralPath = className.toLowerCase();
        if (!className.endsWith("s")){
            pluralPath = className + "s";
        }

        writer.println("    // ========================================================================");
        writer.println("    // CRUD Endpoints");
        writer.println("    // Generated from Class Diagram - Fully functional");
        writer.println("    // ========================================================================");
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

        /* POST /api/entities - Create
        writer.println("    /**");
        writer.println("     * POST /api/" + pluralPath);
        writer.println("     * Create new " + className);
        writer.println("     * @param " + varName + " entity data");
        writer.println("     * @return created entity with 201 status");
        writer.println("     ");
        writer.println("    @PostMapping");
        //writer.println("    public ResponseEntity<" + className + "> create(@Valid @RequestBody " +
        //        className + " " + varName + ") {");
        writer.println("    public ResponseEntity<" + className + "> create(@RequestBody " +
                className + " " + varName + ") {");
        writer.println("        " + className + " saved = " + serviceName + ".save(" + varName + ");");
        writer.println("        return ResponseEntity.status(HttpStatus.CREATED).body(saved);");
        writer.println("    }");
        writer.println();

        // PUT /api/entities/{id} - Update
        writer.println("    /**");
        writer.println("     * PUT /api/" + pluralPath + "/{id}");
        writer.println("     * Update existing " + className);
        writer.println("     * @param id entity id");
        writer.println("     * @param " + varName + " updated data");
        writer.println("     * @return updated entity if found, 404 if not");
        writer.println("     ");
        writer.println("    @PutMapping(\"/{id}\")");
        //writer.println("    public ResponseEntity<" + className + "> update(");
        //writer.println("            @PathVariable Long id,");
        //writer.println("            @Valid @RequestBody " + className + " " + varName + ") {");
        writer.println("    public ResponseEntity<" + className + "> update(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @RequestBody " + className + " " + varName + ") {");
        writer.println();
        writer.println("        if (!" + serviceName + ".existsById(id)) {");
        writer.println("            return ResponseEntity.notFound().build();");
        writer.println("        }");
        writer.println();
        writer.println("        " + varName + ".setId(id);");
        writer.println("        " + className + " updated = " + serviceName + ".save(" + varName + ");");
        writer.println("        return ResponseEntity.ok(updated);");
        writer.println("    }");
        writer.println();*/

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
        String serviceName = cls.getName().toLowerCase() + "Service";
        boolean hasRelationships = false;

        for (Association assoc : associations) {
            // source side
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String targetMul = assoc.getTargetMultiplicity();

                // se for multiplicidade muitos ou for um qualifier(map)
                if (targetMul != null && ( (targetMul.equals("*") || targetMul.contains("..")) || assoc.hasQualifier())) {
                    if (!hasRelationships) {
                        writer.println("    // Relationship management stuff");
                        hasRelationships = true;
                    }

                    String relatedClass = assoc.getTargetClass();
                    generateAddRelationEndpoint(writer, cls.getName(), relatedClass, serviceName);
                    generateRemoveRelationEndpoint(writer, cls.getName(), relatedClass, serviceName, assoc);

                }
            }
        }
    }

    private void generateAddRelationEndpoint(PrintWriter writer, String className, String relatedClass, String serviceName) {
        String relatedPlural = relatedClass + "s";

        writer.println("    /**");
        writer.println("     * Add " + relatedPlural + " (batch operation)");
        writer.println("     */");
        writer.println("    @PostMapping(\"/{id}/" + relatedPlural + "\")");
        writer.println("    public ResponseEntity<?> add" + capitalizeFirstLetter(relatedPlural) + "(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @RequestBody List<" + relatedClass + "> " + relatedPlural + ") {");
        writer.println("        try {");
        writer.println("            var result = " + serviceName + ".add" + capitalizeFirstLetter(relatedPlural) +
                "(id, " + relatedPlural + ");");
        writer.println("            return ResponseEntity.ok(result);");
        writer.println("        } catch (IllegalArgumentException e) {");
        writer.println("            return ResponseEntity.badRequest().body(e.getMessage());");
        writer.println("        }");
        writer.println("    }");
        writer.println();
    }

    private void generateRemoveRelationEndpoint(PrintWriter writer, String className, String relatedClass, String serviceName, Association assoc) {
        String relatedPlural = relatedClass.toLowerCase() + "s";
        String idType = assoc.hasQualifier() ? assoc.getQualifierType() : "Long";
        String idsParam = relatedPlural + "Ids";

        writer.println("    /**");
        writer.println("     * Remove " + relatedPlural + " (batch operation)");
        writer.println("     */");
        writer.println("    @DeleteMapping(\"/{id}/" + relatedPlural + "\")");
        writer.println("    public ResponseEntity<?> remove" + capitalizeFirstLetter(relatedPlural) + "(");
        writer.println("            @PathVariable Long id,");
        writer.println("            @RequestBody List<" + idType + "> " + idsParam + ") {");
        writer.println("        var result = " + serviceName + ".remove" + capitalizeFirstLetter(relatedPlural) +
                "(id, " + idsParam + ");");
        writer.println("        return ResponseEntity.ok(result);");
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

        writer.println();
    }

    private void generateDependencies(PrintWriter writer, String className) {
        writer.println("    private final " + className + "Service " +
                toLowerCaseFirst(className) + "Service;");
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

}
