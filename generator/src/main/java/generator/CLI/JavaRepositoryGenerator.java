package generator.CLI;

import model.Class;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Gera os repositórios do CLI plain (EM MEMÓRIA, sem BD).
 * Raiz: dona do Map<Long,T> (store da família); subclasses: vista instanceof sobre o repo da raiz
 * (mirror do SINGLE_TABLE). Sem transações; ids sempre Long.
 */
/**
 * Para ja gera repositorio em memoria por classe
 * Cada repositório usa um Map<Long, T> com auto-incremento de id,
 * e expõe save / findById / findAll / deleteById / existsById.
 */
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
        String filename = repoDir + "/" + cls.getName() + "Repository.java";
        try (PrintWriter w = new PrintWriter(filename)) {
            if (cls.getParentClass() == null) generateRootRepository(w, cls);   // dono do Map (store da família)
            else                              generateChildRepository(w, cls);  // vista sobre o repo da raiz
        }
        System.out.println("Generated: " + cls.getName() + "Repository.java");
    }

    // usado por classes normais (sem pai)
    // repo RAIZ: dono do Map<Long, Raiz>
    // store partilhado familia se tiver (mirror do SINGLE_TABLE)
    private void generateRootRepository(PrintWriter w, Class cls) {
        String type = cls.getName();
        w.println("package repository;");
        w.println();
        w.println("import models." + type + ";");
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
    // repo filho sem Map próprio — vista sobre o rep da RAIZ para
    // que rootRepository.findAll() veja também os subtipos
    private void generateChildRepository(PrintWriter w, Class cls) {
        String type = cls.getName();
        String root = cls.getRoot().getName();
        w.println("package repository;");
        w.println();
        w.println("import models." + type + ";");
        w.println("import models." + root + ";");
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

