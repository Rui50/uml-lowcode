package generator.CLI;

import model.Class;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * gera os repositórios em mem do CLI (sem BD), um por classe.
 * A classe raiz tem o Map<Long,T> , subclasses reutilizam o da raiz
 * cada repo tem save/findById/findAll/deleteById/existsById.
 */
public class JavaRepositoryGenerator {
    private String baseOutputPath;

    private String basePackage;

    public JavaRepositoryGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    public void generateRepositories(List<Class> classes) throws IOException {
        System.out.println("Generating Java repositories...");

        String repoPath = baseOutputPath + "/src/" + basePackage.replace('.', '/') + "/repository";
        new File(repoPath).mkdirs();

        for (Class cls : classes) {
            generateRepository(cls, repoPath);
        }

        System.out.println("Java repository generation completed.");
    }

    private void generateRepository(Class cls, String repoDir) throws IOException {
        String filename = repoDir + "/" + cls.getName() + "Repository.java";
        try (PrintWriter w = new PrintWriter(filename)) {
            if (cls.getParentClass() == null) generateRootRepository(w, cls);   // dono do Map (store da hierarquia)
            else                              generateChildRepository(w, cls);  // vista sobre o repo da raiz
        }
        System.out.println("Generated: " + cls.getName() + "Repository.java");
    }

    // usado por classes normais (sem pai)
    // store partilhado familia se tiver (tipo SINGLE_TABLE)
    private void generateRootRepository(PrintWriter w, Class cls) {
        String type = cls.getName();
        w.println("package " + basePackage + ".repository;");
        w.println();
        w.println("import " + basePackage + ".models." + type + ";");
        w.println("import java.util.*;");
        w.println();
        w.println("public class " + type + "Repository {");
        w.println();
        w.println("    private Map<Long, " + type + "> data = new HashMap<>();");
        w.println("    private Long nextId = 1L;");
        w.println();
        w.println("    public " + type + " save(" + type + " entity) {");
        w.println("        if (entity.getId() == null) {");
        w.println("            entity.setId(nextId++);");
        w.println("        }");
        w.println("        data.put(entity.getId(), entity);");
        w.println("        return entity;");
        w.println("    }");
        w.println();
        w.println("    public " + type + " findById(Long id) {");
        w.println("        return data.get(id);");
        w.println("    }");
        w.println();
        w.println("    public List<" + type + "> findAll() {");
        w.println("        return new ArrayList<>(data.values());");
        w.println("    }");
        w.println();
        w.println("    public boolean deleteById(Long id) {");
        w.println("        return data.remove(id) != null;");
        w.println("    }");
        w.println();
        w.println("    public boolean existsById(Long id) {");
        w.println("        return data.containsKey(id);");
        w.println("    }");
        w.println();
        w.println("}");
    }

    // usado só por subclasses
    private void generateChildRepository(PrintWriter w, Class cls) {
        String type = cls.getName();
        String root = cls.getRoot().getName();
        w.println("package " + basePackage + ".repository;");
        w.println();
        w.println("import " + basePackage + ".models." + type + ";");
        w.println("import " + basePackage + ".models." + root + ";");
        w.println("import java.util.*;");
        w.println("import java.util.stream.Collectors;");
        w.println();
        w.println("public class " + type + "Repository {");
        w.println();
        w.println("    private final " + root + "Repository root;");
        w.println();
        w.println("    public " + type + "Repository(" + root + "Repository root) {");
        w.println("        this.root = root;");
        w.println("    }");
        w.println();
        w.println("    public " + type + " save(" + type + " entity) {");
        w.println("        root.save(entity);");
        w.println("        return entity;");
        w.println("    }");
        w.println();
        w.println("    public " + type + " findById(Long id) {");
        w.println("        " + root + " found = root.findById(id);");
        w.println("        return (found instanceof " + type + ") ? (" + type + ") found : null;");
        w.println("    }");
        w.println();
        w.println("    public List<" + type + "> findAll() {");
        w.println("        return root.findAll().stream()");
        w.println("                .filter(e -> e instanceof " + type + ")");
        w.println("                .map(e -> (" + type + ") e)");
        w.println("                .collect(Collectors.toList());");
        w.println("    }");
        w.println();
        w.println("    public boolean deleteById(Long id) {");
        w.println("        return findById(id) != null && root.deleteById(id);");
        w.println("    }");
        w.println();
        w.println("    public boolean existsById(Long id) {");
        w.println("        return findById(id) != null;");
        w.println("    }");
        w.println();
        w.println("}");
    }
}

