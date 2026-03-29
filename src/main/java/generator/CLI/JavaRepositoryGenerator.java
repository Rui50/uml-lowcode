package generator.CLI;

import model.Class;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class JavaRepositoryGenerator {
    private String baseOutputPath;

    public JavaRepositoryGenerator(String baseOutputPath) {
        this.baseOutputPath = baseOutputPath;
    }
    public void generateRepositories(List<Class> classes) throws IOException {
        System.out.println("Generating Java repositories...");

        String repoPath = baseOutputPath + "/src/repository";
        new File(repoPath).mkdirs();

        for (Class cls : classes) {
            generateRepository(cls, repoPath);
        }

        System.out.println("Java repository generation completed.");
    }

    private void generateRepository(Class cls, String repoDir) throws IOException {
        String className = cls.getName() + "Repository";
        String filename = repoDir + "/" + className + ".java";

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package repository;");
            w.println();
            w.println("import models." + cls.getName() + ";");
            w.println("import java.util.*;");
            w.println();
            w.println("public class " + className + " {");
            w.println();
            w.println("    private Map<Long, " + cls.getName() + "> data = new HashMap<>();");
            w.println("    private Long nextId = 1L;");
            w.println();

            // save
            w.println("    public " + cls.getName() + " save(" + cls.getName() + " entity) {");
            w.println("        if (entity.getId() == null) {");
            w.println("            entity.setId(nextId++);");
            w.println("        }");
            w.println("        data.put(entity.getId(), entity);");
            w.println("        return entity;");
            w.println("    }");
            w.println();

            // findById
            w.println("    public " + cls.getName() + " findById(Long id) {");
            w.println("        return data.get(id);");
            w.println("    }");
            w.println();

            // findAll
            w.println("    public List<" + cls.getName() + "> findAll() {");
            w.println("        return new ArrayList<>(data.values());");
            w.println("    }");
            w.println();

            // deleteById
            w.println("    public boolean deleteById(Long id) {");
            w.println("        return data.remove(id) != null;");
            w.println("    }");
            w.println();

            // existsById
            w.println("    public boolean existsById(Long id) {");
            w.println("        return data.containsKey(id);");
            w.println("    }");
            w.println();

            w.println("}");
        }

        System.out.println("Generated: " + className + ".java");
    }
}

