package generator.spring;

import model.Association;
import model.Class;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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
            generateCrudMethods(writer, className);

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

    private void generateCrudMethods(PrintWriter writer, String className) {
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

}
