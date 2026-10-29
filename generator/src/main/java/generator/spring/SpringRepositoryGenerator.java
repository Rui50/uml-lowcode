package generator.spring;

import generator.GeneratorUtils;
import model.Class;
import model.Attribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Gera os repositórios Spring com @Repository na classe
 * JpaRepository<Entity, IdType>.
 * (save/findById/findAll/deleteById/count/existsById) é fornecido pelo Spring Data.
 */
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
        String repositoryDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/repository";
        new File(repositoryDir).mkdirs();

        for (Class cls : classes){
            generateRepository(cls, repositoryDir);
        }
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
            if ("UUID".equals(cls.getRoot().getIdAttributeType())) writer.println("import java.util.UUID;");   // só quando o id é UUID
            writer.println();

            // Interface
            writer.println("@Repository");
            // tipo do id
            writer.println("public interface " + className + "Repository extends JpaRepository<" +
                    className + ", " + cls.getRoot().getIdAttributeType() + "> {");
            writer.println();

            // query metodos, para ja criamos alguns para atributos String
            generateQueryMethods(writer, cls);

            writer.println("}");
        }
    }

    private void generateQueryMethods(PrintWriter writer, Class cls) {
        writer.println("    //  methods by JpaRepository:");
        writer.println("    // - save(entity)");
        writer.println("    // - findById(id)");
        writer.println("    // - findAll()");
        writer.println("    // - deleteById(id)");
        writer.println("    // - count()");
        writer.println("    // - existsById(id)");
        writer.println();

        /*
        // gerar query methods para atributos String
        boolean hasStringAttributes = false;

        for (Attribute attr : cls.getAllAttributes()) {
            if (attr.getType().equals("String")) {
                if (!hasStringAttributes) {
                    writer.println("    // Query methods (Spring generates implementation):");
                    hasStringAttributes = true;
                }

                String attrName = attr.getName();
                String capitalizedAttr = GeneratorUtils.capitalizeFirst(attrName);

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
        */
    }
}
