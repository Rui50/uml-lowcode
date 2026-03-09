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
            generateCrudMethods(writer, className, cls);

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
        writer.println("import lombok.RequiredArgsConstructor;");
        writer.println("import org.springframework.stereotype.Service;");
        writer.println("import org.springframework.transaction.annotation.Transactional;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println("import java.util.Optional;");
        writer.println();

        // importar os modelos das entidades se necessário
        List<String> modelsToImport = getModelsToImportFromOperations(cls, classes);

        if (!modelsToImport.isEmpty()) {
            writer.println();
            for (String entityName : modelsToImport) {
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


    private void generateDependencies(PrintWriter writer, String className, List<Association> associations, List<Class> classes) {
        // gerar a dependência do repositório
        writer.println("    // Dependencies");
        writer.println("    private final " + className + "Repository " +
                className.toLowerCase() + "Repository;");

        // TODO: Adicionar repositories de associações se necessário
        // Por agora, só o repository principal

        writer.println();
    }

    private void generateCrudMethods(PrintWriter writer, String className, Class cls) {
        String varName = className.toLowerCase();
        String repoName = varName + "Repository";

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

        // para ja if gigante
        if (cls.getInvariants() != null && !cls.getInvariants().isEmpty()) {

            // save()
            writer.println("    /**");
            writer.println("     * @param " + varName + " the entity to save");
            writer.println("     * @return the saved entity");
            writer.println("     */");
            writer.println("    public " + className + " save(" + className + " " + varName + ") {");
            writer.println("        // ocl validation");
            writer.println("        List<String> errors = " + varName + ".validate();");
            writer.println("        if (!errors.isEmpty()) {");
            writer.println("            throw new IllegalArgumentException(");
            writer.println("                \"Validation failed: \" + String.join(\", \", errors)");
            writer.println("            );");
            writer.println("        }");
            writer.println();
            writer.println("        return " + repoName + ".save(" + varName + ");");
            writer.println("    }");
            writer.println();
        }
        else {
            // save()
            writer.println("    /**");
            writer.println("     * @param " + varName + " the entity to save");
            writer.println("     * @return the saved entity");
            writer.println("     */");
            writer.println("    public " + className + " save(" + className + " " + varName + ") {");
            writer.println("        // TODO: Add validation logic here (OCL constraints)");
            writer.println("        return " + repoName + ".save(" + varName + ");");
            writer.println("    }");
            writer.println();
        }

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
    private void generateCustomOperations(PrintWriter writer, Class cls) {
        if (cls.getOperations().isEmpty()) {
            return;
        }

        writer.println("    // Custom operations from class diagram (TODO: implement)");

        for (Operation op : cls.getOperations()) {
            generateCustomOperation(writer, cls, op);
        }
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

}
