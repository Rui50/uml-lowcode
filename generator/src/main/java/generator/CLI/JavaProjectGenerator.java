package generator.CLI;

import model.Association;
import model.Class;
import model.Enumeration;
import model.sequence.Interaction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * gera a estrutura do projeto CLI.
 */
public class JavaProjectGenerator {
    private String outputDirectory;

    public JavaProjectGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * 1 - estrutura (wipe e criar pastas)
     * 2 - main e util
     * 3 - entidades
     * 4 - repos
     * 5 - menus
     */

    public void generate(List<Class> classes, List<Association> associations, List<Enumeration> enumerations, List<Interaction> interactions) throws IOException {
        System.out.println("Generating Java CLI App...");

        // limpar output anterior
        File output = new File(outputDirectory);
        if (output.exists()) {
            deleteDirectory(output);
        }

        // criar estrutura base
        new File(outputDirectory + "/src/util").mkdirs();

        // Main.java + InputUtils.java
        generateMainClass();
        generateInputUtils();

        // entidades
        JavaEntityGenerator entityGenerator = new JavaEntityGenerator(outputDirectory, interactions, enumerations);
        entityGenerator.generateEntities(classes, associations, enumerations);

        // repositórios
        JavaRepositoryGenerator repoGenerator = new JavaRepositoryGenerator(outputDirectory);
        repoGenerator.generateRepositories(classes);

        // menus
        JavaMenuGenerator menuGenerator = new JavaMenuGenerator(outputDirectory, interactions);
        menuGenerator.generateMenus(classes, associations, enumerations);

        System.out.println("CLI Application generated in: " + outputDirectory);
    }

    /**
     * Gera main.java, so cria um MainMenu e chama run()
     */
    private void generateMainClass() throws IOException {
        String filename = outputDirectory + "/src/Main.java";

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("import menus.MainMenu;");
            w.println();
            w.println("public class Main {");
            w.println("    public static void main(String[] args) {");
            w.println("        MainMenu mainMenu = new MainMenu();");
            w.println("        mainMenu.run();");
            w.println("    }");
            w.println("}");
        }

        System.out.println("Main class generated.");

    }

    /**
     * gera InputUtils.java — métodos  para ler input do utilizador.
     */
    private void generateInputUtils() throws IOException {
        String filename = outputDirectory + "/src/util/InputUtils.java";

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package util;");
            w.println();
            w.println("import java.util.Scanner;");
            w.println();
            w.println("public class InputUtils {");
            w.println();
            w.println("    public static int readInt(Scanner scanner, String prompt) {");
            w.println("        System.out.print(prompt);");
            w.println("        while (!scanner.hasNextInt()) {");
            w.println("            System.out.print(\"Invalid. \" + prompt);");
            w.println("            scanner.next();");
            w.println("        }");
            w.println("        int value = scanner.nextInt();");
             w.println("        scanner.nextLine();");
            w.println("        return value;");
            w.println("    }");
            w.println();
            w.println("    public static Long readLong(Scanner scanner, String prompt) {");
            w.println("        System.out.print(prompt);");
            w.println("        while (!scanner.hasNextLong()) {");
            w.println("            System.out.print(\"Invalid. \" + prompt);");
            w.println("            scanner.next();");
            w.println("        }");
            w.println("        Long value = scanner.nextLong();");
            w.println("        scanner.nextLine();");
            w.println("        return value;");
            w.println("    }");
            w.println();
            w.println("    public static double readDouble(Scanner scanner, String prompt) {");
            w.println("        System.out.print(prompt);");
            w.println("        while (!scanner.hasNextDouble()) {");
            w.println("            System.out.print(\"Invalid. \" + prompt);");
            w.println("            scanner.next();");
            w.println("        }");
            w.println("        double value = scanner.nextDouble();");
            w.println("        scanner.nextLine();");
            w.println("        return value;");
            w.println("    }");
            w.println("}");
        }

        System.out.println("Generated: InputUtils.java");
    }

    // apaga recursivamente
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

}
