package generator.CLI_JPA;

import model.Class;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Gera os repositórios JPA (CLI-JPA) — um por classe, a envolver o EntityManager partilhado.
 * save (persist/merge), findById (em.find), findAll (JPQL "from X"), deleteById (find + remove).
 *
 * Notas:
 *  - repos planos: a herança é resolvida pelo JPA (discriminador), sem split raiz/filho;
 *  - id (tipo/nome) resolvido pela raiz — usado em findById/deleteById e no getter do save();
 *  - a transação é do que chama (menu): persist/remove precisam de tx ativa (RESOURCE_LOCAL).
 *
 * lim:
 *  - save() assume id gerado pelo servidor (null no create); ids String atribuídos não são suportados
 *    (o frontend não os expõe no CLI-JPA).
 */
// id: findById/deleteById e o getter usado no save() usam o tipo/nome do id resolvido pela raiz
public class CLIRepositoryGenerator {
    private final String baseOutputPath;
    private final String basePackage;

    public CLIRepositoryGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }
    public void generateRepositories(List<Class> classes) throws IOException {
        System.out.println("Generating CLI-JPA repositories...");
        String repoDir = baseOutputPath + "/src/main/java/"
                + basePackage.replace('.', '/') + "/repository";
        new File(repoDir).mkdirs();

        for (Class cls : classes) {
            generateRepository(cls, repoDir);
        }

        System.out.println("Repository generation completed.");
    }

    private void generateRepository(Class cls, String repoDir) throws IOException {
        String name = cls.getName();
        String filename = repoDir + "/" + name + "Repository.java";

        // id
        String idType = cls.getRoot().getIdAttributeType();
        String idField = cls.getRoot().getIdAttributeName() == null ? "id" : cls.getRoot().getIdAttributeName();
        String idGetter = "get" + Character.toUpperCase(idField.charAt(0)) + idField.substring(1);

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package " + basePackage + ".repository;");
            w.println();
            w.println("import " + basePackage + ".models." + name + ";");
            w.println("import jakarta.persistence.EntityManager;");
            w.println("import java.util.List;");
            if ("UUID".equals(idType)) w.println("import java.util.UUID;");   // só quando o id é UUID
            w.println();
            w.println("public class " + name + "Repository {");
            w.println();
            w.println("    private final EntityManager em;");
            w.println();
            w.println("    public " + name + "Repository(EntityManager em) {");
            w.println("        this.em = em;");
            w.println("    }");
            w.println();
            // save() assume id gerado pelo servidor (null no create -> persist, senão merge).
            // não funciona com ids String atribuídos pelo cliente — o frontend não os expõe no CLI-JPA.
            w.println("    public " + name + " save(" + name + " entity) {");
            w.println("        if (entity." + idGetter + "() == null) {");
            w.println("            em.persist(entity);");
            w.println("            return entity;");
            w.println("        }");
            w.println("        return em.merge(entity);");
            w.println("    }");
            w.println();
            w.println("    public " + name + " findById(" + idType + " id) {");
            w.println("        return em.find(" + name + ".class, id);");
            w.println("    }");
            w.println();
            w.println("    public List<" + name + "> findAll() {");
            w.println("        return em.createQuery(\"from " + name + "\", " + name +
                    ".class).getResultList();");
            w.println("    }");
            w.println();
            w.println("    public void deleteById(" + idType + " id) {");
            w.println("        " + name + " entity = em.find(" + name + ".class, id);");
            w.println("        if (entity != null) {");
            w.println("            em.remove(entity);");
            w.println("        }");
            w.println("    }");
            w.println();
            w.println("}");
        }
        System.out.println("Generated: " + name + "Repository.java");
    }
}
