package generator.CLI;

import generator.GeneratorUtils;

import model.Association;
import model.Class;
import model.Enumeration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Gera a estrutura do projeto CLI plain:
 * Main e InputUtils e geradores de entity/repository/menu.
 */
public class JavaProjectGenerator {
    private String outputDirectory;
    private final String basePackage;

    public JavaProjectGenerator(String outputDirectory, String basePackage) {
        this.outputDirectory = outputDirectory;
        this.basePackage = basePackage;
    }

    /**
     * 1 - estrutura (wipe e criar pastas)
     * 2 - main e util
     * 3 - entidades
     * 4 - repos
     * 5 - menus
     */

    public void generate(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating Java CLI App...");

        // limpar output anterior
        File output = new File(outputDirectory);
        if (output.exists()) {
            GeneratorUtils.deleteDirectory(output);
        }

        // criar estrutura base
        new File(outputDirectory + "/src/" + basePackage.replace('.', '/') + "/util").mkdirs();

        // Main.java + InputUtils.java
        generateMainClass();
        generateInputUtils();

        // entidades
        JavaEntityGenerator entityGenerator = new JavaEntityGenerator(outputDirectory, basePackage);
        entityGenerator.generateEntities(classes, associations, enumerations);

        // repositórios
        JavaRepositoryGenerator repoGenerator = new JavaRepositoryGenerator(outputDirectory, basePackage);
        repoGenerator.generateRepositories(classes);

        // menus
        JavaMenuGenerator menuGenerator = new JavaMenuGenerator(outputDirectory, basePackage);
        menuGenerator.generateMenus(classes, associations, enumerations);

        System.out.println("CLI Application generated in: " + outputDirectory);
    }

    /**
     * gera main.java, so cria um MainMenu e chama run()
     */
    private void generateMainClass() throws IOException {
        String filename = outputDirectory + "/src/" + basePackage.replace('.', '/') + "/Main.java";

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package " + basePackage + ";");
            w.println();
            w.println("import " + basePackage + ".menus.MainMenu;");
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
        String filename = outputDirectory + "/src/" + basePackage.replace('.', '/') + "/util/InputUtils.java";

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package " + basePackage + ".util;");
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
            w.println();
            w.println("    public static float readFloat(Scanner scanner, String prompt) {");
            w.println("        System.out.print(prompt);");
            w.println("        while (!scanner.hasNextFloat()) {");
            w.println("            System.out.print(\"Invalid. \" + prompt);");
            w.println("            scanner.next();");
            w.println("        }");
            w.println("        float value = scanner.nextFloat();");
            w.println("        scanner.nextLine();");
            w.println("        return value;");
            w.println("    }");
            w.println();
            // por linha (o Scanner não tem hasNextBigDecimal
            w.println("    public static java.math.BigDecimal readBigDecimal(Scanner scanner, String prompt) {");
            w.println("        while (true) {");
            w.println("            System.out.print(prompt);");
            w.println("            try { return new java.math.BigDecimal(scanner.nextLine().trim()); }");
            w.println("            catch (NumberFormatException e) { System.out.println(\"Invalid number.\"); }");
            w.println("        }");
            w.println("    }");
            w.println();
            w.println("    public static java.math.BigInteger readBigInteger(Scanner scanner, String prompt) {");
            w.println("        while (true) {");
            w.println("            System.out.print(prompt);");
            w.println("            try { return new java.math.BigInteger(scanner.nextLine().trim()); }");
            w.println("            catch (NumberFormatException e) { System.out.println(\"Invalid number.\"); }");
            w.println("        }");
            w.println("    }");
            w.println("}");
        }

        System.out.println("Generated: InputUtils.java");
    }


}
