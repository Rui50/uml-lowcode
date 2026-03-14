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
            generateQueryMethods(writer, cls);

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
        // gerar a dependência do repositório
        writer.println("    // Dependencies");
        writer.println("    private final " + className + "Repository " +
                className.toLowerCase() + "Repository;");

        // TODO: Adicionar repositories de associações se necessário
        // Por agora, só o repository principal

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
        // para o write do comment inicial
        boolean hasRelationships = false;

        for (Association assoc : associations) {
            // se estivermos na source e for navegável para o target
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()){
                String targetMul = assoc.getTargetMultiplicity();

                // se for multiplicidade muitos ou for um qualifier(map)
                if (targetMul != null && ( (targetMul.equals("*") || targetMul.contains("..")) || assoc.hasQualifier())) {
                    if (!hasRelationships) {
                        writer.println("    // Relationship management stuff");
                        hasRelationships = true;
                    }

                    String relatedClass = assoc.getTargetClass();
                    String fieldName = makeAttributeName(relatedClass, assoc.getTargetRole(), targetMul);

                    generateAddRelationMethod(writer, cls, findClassByName(relatedClass, classes), fieldName, assoc);
                    generateRemoveRelationMethod(writer, cls.getName(), relatedClass, fieldName, assoc);
                }
            }

            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String sourceMul = assoc.getSourceMultiplicity();

                if (sourceMul != null && ((sourceMul.equals("*") || sourceMul.contains("..")) || assoc.hasQualifier())) {
                    if (!hasRelationships) {
                        writer.println("    // Relationship management stuff");
                        hasRelationships = true;
                    }

                    String relatedClass = assoc.getSourceClass();
                    String fieldName = makeAttributeName(relatedClass, assoc.getSourceRole(), sourceMul);

                    generateAddRelationMethod(writer, cls, findClassByName(relatedClass, classes), fieldName, assoc);
                    generateRemoveRelationMethod(writer, cls.getName(), relatedClass, fieldName, assoc);
                }
            }
        }
    }

    private void generateAddRelationMethod(PrintWriter writer, Class cls, Class relatedCls, String fieldName, Association assoc) {
        String className = cls.getName();
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";

        String relatedClass = relatedCls.getName();
        String relatedVar = relatedClass.toLowerCase();
        String relatedPlural = relatedClass.toLowerCase() + "s"; // simplificação, para pluralizar o nome da variável

        writer.println("    /**");
        writer.println("     * Add a " + relatedPlural + " to " + className);
        writer.println("     * @param id the " + className + " id");
        writer.println("     * @param " + relatedPlural + " list of " + relatedClass + " to add");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " add" + capitalizeFirstLetter(relatedPlural) +
                "(Long id, List<" + relatedClass + "> " + relatedPlural + ") {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + id));");
        writer.println();

        writer.println("        // Add all (so it works for batch operation)");
        writer.println("        for (" + relatedClass + " " + relatedVar + " : " + relatedPlural + ") {");

        // verificar se tem OCL
        if (relatedCls.getInvariants() != null && !relatedCls.getInvariants().isEmpty()) {
            // validar entidade
            writer.println("            // Validate related entity");
            writer.println("            List<String> errors = " + relatedVar + ".validate();");
            writer.println("            if (!errors.isEmpty()) {");
            writer.println("                throw new IllegalArgumentException(");
            writer.println("                    \"Validation failed for " + relatedClass + ": \" + errors");
            writer.println("                );");
            writer.println("            }");
            writer.println();
        }

        // add na coleção
        String getter = "get" + capitalizeFirstLetter(fieldName);
        if (assoc.hasQualifier()) {
            // Map - precisa da chava
            String qualifierName = assoc.getQualifierName();
            String qualifierGetter = "get" + capitalizeFirstLetter(qualifierName);
            writer.println("            " + varName + "." + getter + "().put(" +
                    relatedVar + "." + qualifierGetter + "(), " + relatedVar + ");");
        } else {
            // List
            writer.println("            " + varName + "." + getter + "().add(" + relatedVar + ");");
        }


        writer.println("        }");
        writer.println();
        if (cls.getInvariants() != null && !cls.getInvariants().isEmpty()) {
            writer.println("        // Validate parent entity after adding");
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

    private void generateRemoveRelationMethod(PrintWriter writer, String className, String relatedClass, String fieldName, Association assoc) {
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";

        String relatedVar = relatedClass.toLowerCase();
        String relatedPlural = relatedClass.toLowerCase() + "s"; // simplificação,

        String idType = assoc.hasQualifier() ? assoc.getQualifierType() : "Long";
        String idsParam = relatedVar + "Ids";

        writer.println("    /**");
        writer.println("     * Remove a " + relatedClass + " from " + className);
        writer.println("     * @param id the " + className + " id");
        writer.println("     * @param " + idsParam + " the ids of the " + relatedClass + " to remove");
        writer.println("     * @return the updated " + className);
        writer.println("     */");
        writer.println("    public " + className + " remove" + capitalizeFirstLetter(relatedPlural) +
                "(Long id, List<" + idType + "> " + idsParam + ") {");
        writer.println("        " + className + " " + varName + " = " + repoName + ".findById(id)");
        writer.println("            .orElseThrow(() -> new RuntimeException(\"" + className +
                " not found: \" + id));");
        writer.println();
        writer.println("        // Remove all (batch operation)");

        String getter = "get" + capitalizeFirstLetter(fieldName);
        if (assoc.hasQualifier()) {
            // map - remove by key
            writer.println("        for (" + idType + " " + relatedVar + "Id : " + idsParam + ") {");
            writer.println("            " + varName + "." + getter + "().remove(" + relatedVar + "Id);");
            writer.println("        }");
        } else {
            // list - remove by entity
            /*writer.println("        for (" + idType + " " + relatedVar + "Id : " + idsParam + ") {");
            writer.println("            " + varName + "." + getter + "().removeIf(" +
                    relatedVar + " -> " + relatedVar + ".getId().equals(" + relatedVar + "Id));");
            writer.println("        }");*/
            writer.println("        " + varName + "." + getter + "().removeIf(" +
                    relatedClass.toLowerCase() + " -> " + idsParam + ".contains(" +
                    relatedClass.toLowerCase() + ".getId()));");
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

        // create()
        writer.println("    /**");
        writer.println("     * @param " + varName + " the entity to create");
        writer.println("     * @return the created entity");
        writer.println("     */");
        writer.println("    public " + className + " create(" + className + " " + varName + ") {");

        // Inicializar coleções if null
        boolean hasCollections = false; // isto simplesmente é para apenas comentar uma vez
        for (Association assoc : associations) {
            // ver na source
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String targetMultiplicity = assoc.getTargetMultiplicity();
                if (targetMultiplicity != null && (targetMultiplicity.equals("*") || targetMultiplicity.contains(".."))) {
                    String fieldName = makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), targetMultiplicity);

                    String should_init = assoc.hasQualifier() ? "new HashMap<>()" : "new ArrayList<>()";

                    if (!hasCollections) {
                        writer.println("        // init collections for associations");
                        hasCollections = true;
                    }

                    writer.println("        if (" + varName + ".get" + capitalizeFirstLetter(fieldName) +
                            "() == null) " + varName + ".set" + capitalizeFirstLetter(fieldName) +
                            "(" + should_init + ");");
                }
            }
            // agora target mesma coisa
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()){
                String sourceMultiplicity = assoc.getSourceMultiplicity();
                if (sourceMultiplicity != null && (sourceMultiplicity.equals("*") || sourceMultiplicity.contains(".."))) {
                    String fieldName = makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), sourceMultiplicity);

                    String should_init = assoc.hasQualifier() ? "new HashMap<>()" : "new ArrayList<>()";

                    if (!hasCollections) {
                        writer.println("        // Initialize collections for associations");
                        hasCollections = true;
                    }

                    writer.println("        if (" + varName + ".get" + capitalizeFirstLetter(fieldName) +
                            "() == null) " + varName + ".set" + capitalizeFirstLetter(fieldName) +
                            "(" + should_init + ");");
                }
            }
        }

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

    private String makeAttributeName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }

        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        return checkMultiplicity(multiplicity) ? base + "s" : base;
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

    private Class findClassByName(String name, List<Class> classes) {
        for (Class cls : classes) {
            if (cls.getName().equals(name)) {
                return cls;
            }
        }
        return null;
    }

}
