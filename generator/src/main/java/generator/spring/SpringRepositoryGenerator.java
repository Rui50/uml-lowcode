package generator.spring;

import model.Class;
import model.Attribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

// id: JpaRepository<Entity, T>, em que T = tipo do id (cls.getRoot().getIdAttributeType())
public class SpringRepositoryGenerator {
    private String baseOutputPath;
    private String basePackage;

    public SpringRepositoryGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    /**
     * Gera os repositórios para as classes de modelo.
     * */
    public void generateRepositories(List<Class> classes) throws IOException {
        // Lógica para gerar os repositórios usando o Spring Data JPA
        // Para cada classe de modelo, criar uma interface de repositório
        // que estende JpaRepository ou CrudRepository
        System.out.println("Generating Spring Data JPA repositories...");

        String repositoryDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/repository";
        new File(repositoryDir).mkdirs();

        for (Class cls : classes){
            generateRepository(cls, repositoryDir);
        }

        System.out.println("Generation of repositories completed.");
    }

    // gerar um repositorio
    private void generateRepository(Class cls, String repositoryDir) throws IOException {
        String className = cls.getName();
        String filename = repositoryDir + "/" + className + "Repository.java";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // package
            writer.println("package " + basePackage + ".repository;");
            writer.println();

            // Imports
            writer.println("import " + basePackage + ".models." + className + ";");
            writer.println("import org.springframework.data.jpa.repository.JpaRepository;");
            writer.println("import org.springframework.stereotype.Repository;");
            writer.println("import java.util.List;");
            writer.println("import java.util.UUID;");
            writer.println();

            // Interface
            writer.println("/**");
            writer.println(" * Repository for " + className + " entity");
            writer.println(" * Spring Data JPA generates a base implementation automatically");
            writer.println(" */");
            writer.println("@Repository");
            // tipo do id partir da raiz da hierarquia
            writer.println("public interface " + className + "Repository extends JpaRepository<" +
                    className + ", " + cls.getRoot().getIdAttributeType() + "> {");
            writer.println();

            // query metodos, para ja criamos alguns para atributos String
            generateQueryMethods(writer, cls);

            writer.println("}");
        }

        System.out.println("Generated repository: " + filename);
    }

    private void generateQueryMethods(PrintWriter writer, Class cls) {
        writer.println("    // Automatic CRUD methods provided by JpaRepository:");
        writer.println("    // - save(entity)");
        writer.println("    // - findById(id)");
        writer.println("    // - findAll()");
        writer.println("    // - deleteById(id)");
        writer.println("    // - count()");
        writer.println("    // - existsById(id)");
        writer.println();

        // gerar query methods para atributos String
        boolean hasStringAttributes = false;

        for (Attribute attr : cls.getAllAttributes()) {
            if (attr.getType().equals("String")) {
                if (!hasStringAttributes) {
                    writer.println("    // Query methods (Spring generates implementation):");
                    hasStringAttributes = true;
                }

                String attrName = attr.getName();
                String capitalizedAttr = attrName.substring(0, 1).toUpperCase() + attrName.substring(1);

                writer.println("    List<" + cls.getName() + "> findBy" + capitalizedAttr +
                        "(String " + attrName + ");");
                writer.println("    List<" + cls.getName() + "> findBy" + capitalizedAttr +
                        "Containing(String " + attrName + ");");
                writer.println("    boolean existsBy" + capitalizedAttr + "(String " + attrName + ");");
                writer.println();
            }
        }

        if (!hasStringAttributes) {
            writer.println("    // custom query methods TODO");
            writer.println();
        }
    }
}
