package generator.CLI_JPA;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CLIProjectGenerator {
    private final CLIProjectSettings settings;

    private final String outputDirectory;

    public CLIProjectGenerator(CLIProjectSettings settings, String outputDirectory) {
        this.settings = settings;
        this.outputDirectory = outputDirectory;
    }
    /**
     * 1 - wipe output
     * 2 - criar layout maven (src/main/java/<package>, src/main/resources/META-INF)
     * 3 - pom.xml      (próximo passo)
     * 4 - persistence.xml (próximo passo)
     */
    public void generateProjectStructure() throws IOException {
        System.out.println("Generating CLI-JPA App...");

        //  wipe
        File output = new File(outputDirectory);
        if (output.exists()) {
            deleteDirectory(output);
        }

        //  layout
        String javaDir = outputDirectory + "/src/main/java/" + settings.getPackagePath();
        String resourcesDir = outputDirectory + "/src/main/resources/META-INF";

        new File(javaDir).mkdirs();
        new File(javaDir + "/models").mkdirs();
        new File(javaDir + "/repository").mkdirs();
        new File(javaDir + "/menus").mkdirs();
        new File(javaDir + "/util").mkdirs();
        new File(resourcesDir).mkdirs();

        generatePomXml();
        generatePersistenceXml();
        generateConfigProperties();
        generateMainClass();
        generateInputUtils();


        System.out.println("CLI-JPA project structure created in: " + outputDirectory);
    }

    private void generatePomXml() throws IOException {
        String filename = outputDirectory + "/pom.xml";
        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {

            String basePackage = settings.getBasePackage();
            int lastDot = basePackage.lastIndexOf('.');
            String groupId    = lastDot > 0 ? basePackage.substring(0, lastDot) : basePackage;
            String artifactId = lastDot > 0 ? basePackage.substring(lastDot + 1) : basePackage;

            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"");
            w.println("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            w.println("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " +
                    "https://maven.apache.org/xsd/maven-4.0.0.xsd\">");
            w.println("    <modelVersion>4.0.0</modelVersion>");
            w.println();
            w.println("    <groupId>" + groupId + "</groupId>");
            w.println("    <artifactId>" + artifactId + "</artifactId>");
            w.println("    <version>0.0.1-SNAPSHOT</version>");
            w.println();
            w.println("    <properties>");
            w.println("        <maven.compiler.source>17</maven.compiler.source>");
            w.println("        <maven.compiler.target>17</maven.compiler.target>");
            w.println("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>");
            w.println("    </properties>");
            w.println();
            w.println("    <dependencies>");

            w.println("        <dependency>");
            w.println("            <groupId>org.hibernate.orm</groupId>");
            w.println("            <artifactId>hibernate-core</artifactId>");
            w.println("            <version>6.5.2.Final</version>");
            w.println("        </dependency>");

            switch (settings.getDatabaseType()) {
                case H2:
                    w.println("        <dependency>");
                    w.println("            <groupId>com.h2database</groupId>");
                    w.println("            <artifactId>h2</artifactId>");
                    w.println("            <version>2.2.224</version>");
                    w.println("        </dependency>");
                    break;
                case POSTGRESQL:
                    w.println("        <dependency>");
                    w.println("            <groupId>org.postgresql</groupId>");
                    w.println("            <artifactId>postgresql</artifactId>");
                    w.println("            <version>42.7.3</version>");
                    w.println("        </dependency>");
                    break;
                case MYSQL:
                    w.println("        <dependency>");
                    w.println("            <groupId>com.mysql</groupId>");
                    w.println("            <artifactId>mysql-connector-j</artifactId>");
                    w.println("            <version>8.4.0</version>");
                    w.println("        </dependency>");
                    break;
            }

            w.println("    </dependencies>");
            w.println();
            w.println("    <build>");
            w.println("        <plugins>");
            w.println("            <plugin>");
            w.println("                <groupId>org.codehaus.mojo</groupId>");
            w.println("                <artifactId>exec-maven-plugin</artifactId>");
            w.println("                <version>3.3.0</version>");
            w.println("                <configuration>");
            w.println("                    <mainClass>" + basePackage + ".Main</mainClass>");
            w.println("                </configuration>");
            w.println("            </plugin>");
            w.println("        </plugins>");
            w.println("    </build>");
            w.println("</project>");
        }
        System.out.println("Generated: pom.xml");
    }

    private void generatePersistenceXml() throws IOException {
        String filename = outputDirectory + "/src/main/resources/META-INF/persistence.xml";
        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.println("<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\"");
            w.println("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            w.println("             xsi:schemaLocation=\"https://jakarta.ee/xml/ns/persistence " +
                    "https://jakarta.ee/xml/ns/persistence/persistence_3_1.xsd\"");
            w.println("             version=\"3.1\">");
            w.println();
            w.println("    <persistence-unit name=\"app-pu\" transaction-type=\"RESOURCE_LOCAL\">");
            w.println("        <exclude-unlisted-classes>false</exclude-unlisted-classes>");
            w.println("        <properties>");
            w.println("            <property name=\"hibernate.hbm2ddl.auto\" value=\"update\"/>");
            w.println("            <property name=\"hibernate.show_sql\" value=\"true\"/>");
            w.println("        </properties>");
            w.println("    </persistence-unit>");
            w.println("</persistence>");
        }
        System.out.println("Generated: persistence.xml");
    }

    private void generateConfigProperties() throws IOException {
        String filename = outputDirectory + "/src/main/resources/config.properties";
        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            String dbName = settings.getApplicationName();
            switch (settings.getDatabaseType()) {
                case H2 -> {
                    w.println("jakarta.persistence.jdbc.url=jdbc:h2:./data/" + dbName);
                    w.println("jakarta.persistence.jdbc.user=sa");
                    w.println("jakarta.persistence.jdbc.password=");
                }
                case POSTGRESQL -> {
                    w.println("jakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5432/" + dbName);
                    w.println("jakarta.persistence.jdbc.user=uml");
                    w.println("jakarta.persistence.jdbc.password=uml");
                }
                case MYSQL -> {
                    w.println("jakarta.persistence.jdbc.url=jdbc:mysql://localhost:3306/" + dbName);
                    w.println("jakarta.persistence.jdbc.user=root");
                    w.println("jakarta.persistence.jdbc.password=");
                }
            }
        }
        System.out.println("Generated: config.properties");
    }

    private void generateInputUtils() throws IOException {
        String basePackage = settings.getBasePackage();
        String dir = outputDirectory + "/src/main/java/" + settings.getPackagePath() + "/util";
        String filename = dir + "/InputUtils.java";

        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
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
            w.println("}");
        }
        System.out.println("Generated: InputUtils.java");
    }

    private void generateMainClass() throws IOException {
        String basePackage = settings.getBasePackage();
        String dir = outputDirectory + "/src/main/java/" + settings.getPackagePath();
        String filename = dir + "/Main.java";

        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("package " + basePackage + ";");
            w.println();
            w.println("import " + basePackage + ".menus.MainMenu;");
            w.println("import jakarta.persistence.EntityManager;");
            w.println("import jakarta.persistence.EntityManagerFactory;");
            w.println("import jakarta.persistence.Persistence;");
            w.println("import java.io.InputStream;");
            w.println("import java.util.HashMap;");
            w.println("import java.util.Map;");
            w.println("import java.util.Properties;");
            w.println();
            w.println("public class Main {");
            w.println();
            w.println("    public static void main(String[] args) throws Exception {");
            w.println("        EntityManagerFactory emf = buildEntityManagerFactory();");
            w.println("        EntityManager em = emf.createEntityManager();");
            w.println("        try {");
            w.println("            new MainMenu(em).run();");
            w.println("        } finally {");
            w.println("            em.close();");
            w.println("            emf.close();");
            w.println("        }");
            w.println("    }");
            w.println();
            w.println("    private static EntityManagerFactory buildEntityManagerFactory() throws Exception {");
            w.println("        Properties props = new Properties();");
            w.println("        try (InputStream in = Main.class.getResourceAsStream(\"/config.properties\")) {");
            w.println("            props.load(in);");
            w.println("        }");
            w.println("        Map<String, Object> overrides = new HashMap<>();");
            w.println("        for (String name : props.stringPropertyNames()) {");
            w.println("            overrides.put(name, props.getProperty(name));");
            w.println("        }");
            w.println("        return Persistence.createEntityManagerFactory(\"app-pu\", overrides);");
            w.println("    }");
            w.println("}");
        }
        System.out.println("Generated: Main.java");
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