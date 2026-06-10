package generator.spring;

import generator.GeneratorUtils;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Gera o esqueleto do projecto Spring
 * - cria estrutura de pastas
 * - copia templates base (pom.xml, application.properties, main class, test class)
 * - gera pom.xml com dependências (Spring Web, Spring Data JPA, driver do banco)
 * - gera application.properties com configuração de datasource
 * - gera classe principal com @SpringBootApplication
 *
 *  depos os geradores entidades/DTOs/repos/services/controllersescrevem por cima desta base.
 */

public class SpringProjectGenerator {
    private SpringProjectSettings settings;
    private String outputDirectory;
    private String baseTemplateDir = "../generator/templates/spring-boot-static";

    public SpringProjectGenerator(SpringProjectSettings settings, String outputDirectory) {
        this.settings = settings;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Gera a estrutura do projeto Spring Boot, copia a base, gera o pom.xml, application.properties, classe principal e classe de teste.
     * */
    public void generateProjectStructure() throws IOException {
        System.out.println("Generating Spring Boot project structure...");

        File output = new File(outputDirectory);
        if (output.exists()) {
            GeneratorUtils.deleteDirectory(output);
        }

        createDirectoryStructure();
        copyBaseTemplate();
        generatePomXml();
        generateApplicationProperties();
        generateMainClass();
        generateTestClass();
        if (settings.isDockerfile()) {
            generateDockerfile();
            // H2 é embebido mas Postgres/MySQL precisam de um container de BD -> docker-compose
            if (settings.getDatabaseType() != DatabaseType.H2) {
                generateDockerCompose();
            }
        }

        System.out.println("Project structure generated in: " + outputDirectory);
    }

    // Dockerfile, build com Maven, run com JRE (jar executável do spring-boot-maven-plugin)
    private void generateDockerfile() throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(outputDirectory + "/Dockerfile"))) {
            w.println("# build stage");
            w.println("FROM maven:3.9-eclipse-temurin-17 AS build");
            w.println("WORKDIR /app");
            w.println("COPY pom.xml .");
            w.println("COPY src ./src");
            w.println("RUN mvn -q clean package -DskipTests");
            w.println();
            w.println("# run stage");
            w.println("FROM eclipse-temurin:17-jre");
            w.println("WORKDIR /app");
            w.println("COPY --from=build /app/target/*.jar app.jar");
            w.println("EXPOSE " + settings.getServerPort());
            w.println("ENTRYPOINT [\"java\",\"-jar\",\"app.jar\"]");
        }
        System.out.println("Generated: Dockerfile");
    }

    // docker-compose: app + container de BD (Postgres/MySQL). credenciais por
    // env (SPRING_DATASOURCE_*), que o Spring Boot usa por cima do application.properties.
    private void generateDockerCompose() throws IOException {
        String dbName = settings.getApplicationName();
        boolean postgres = settings.getDatabaseType() == DatabaseType.POSTGRESQL;
        int port = settings.getServerPort();
        try (PrintWriter w = new PrintWriter(new FileWriter(outputDirectory + "/docker-compose.yml"))) {
            w.println("services:");
            w.println("  db:");
            if (postgres) {
                w.println("    image: postgres:16");
                w.println("    environment:");
                w.println("      POSTGRES_DB: " + dbName);
                w.println("      POSTGRES_USER: uml");
                w.println("      POSTGRES_PASSWORD: uml");
                w.println("    healthcheck:");
                w.println("      test: [\"CMD-SHELL\", \"pg_isready -U uml -d " + dbName + "\"]");
                w.println("      interval: 5s");
                w.println("      retries: 10");
            } else {
                w.println("    image: mysql:8");
                w.println("    environment:");
                w.println("      MYSQL_DATABASE: " + dbName);
                w.println("      MYSQL_USER: uml");
                w.println("      MYSQL_PASSWORD: uml");
                w.println("      MYSQL_ROOT_PASSWORD: uml");
                w.println("    healthcheck:");
                w.println("      test: [\"CMD\", \"mysqladmin\", \"ping\", \"-h\", \"localhost\"]");
                w.println("      interval: 5s");
                w.println("      retries: 10");
            }
            w.println();
            w.println("  app:");
            w.println("    build: .");
            w.println("    depends_on:");
            w.println("      db:");
            w.println("        condition: service_healthy");
            w.println("    ports:");
            w.println("      - \"" + port + ":" + port + "\"");
            w.println("    environment:");
            String url = postgres
                    ? "jdbc:postgresql://db:5432/" + dbName
                    : "jdbc:mysql://db:3306/" + dbName;
            w.println("      SPRING_DATASOURCE_URL: " + url);
            w.println("      SPRING_DATASOURCE_USERNAME: uml");
            w.println("      SPRING_DATASOURCE_PASSWORD: uml");
        }
        System.out.println("Generated: docker-compose.yml");
    }

    private void createDirectoryStructure() throws IOException {
        String packagePath = settings.getPackagePath();

        String[] dirs = {
            "src/main/java/" + packagePath,
            "src/main/resources",
            "src/test/java/" + packagePath
        };
        for (String dir : dirs) {
            Path path = Path.of(outputDirectory, dir);
            Files.createDirectories(path);
        }
    }

    private void copyBaseTemplate() throws IOException {
        Path source = new File(baseTemplateDir).toPath();
        Path target = new File(outputDirectory).toPath();

        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy: " + sourcePath, e);
            }
        });
    }

    private void generatePomXml() throws IOException {
        String filename = outputDirectory + "/pom.xml";
        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"");
            w.println("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            w.println("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " +
                    "https://maven.apache.org/xsd/maven-4.0.0.xsd\">");
            w.println("    <modelVersion>4.0.0</modelVersion>");
            w.println();
            w.println("    <parent>");
            w.println("        <groupId>org.springframework.boot</groupId>");
            w.println("        <artifactId>spring-boot-starter-parent</artifactId>");
            w.println("        <version>4.0.3</version>");
            w.println("        <relativePath/>");
            w.println("    </parent>");
            w.println();
            // groupId: package sem a ultima parte, artifactId: ultima parte
            String basePackage = settings.getBasePackage();
            int lastDot = basePackage.lastIndexOf('.');
            String groupId = lastDot > 0 ? basePackage.substring(0, lastDot) : basePackage;
            String artifactId = lastDot > 0 ? basePackage.substring(lastDot + 1) : basePackage;
            //spring-boot-starter-webmvc | spring-boot-starter-web decidir
            w.println("    <groupId>" + groupId + "</groupId>");
            w.println("    <artifactId>" + artifactId + "</artifactId>");
            w.println("    <version>0.0.1-SNAPSHOT</version>");
            w.println("    <name>" + settings.getApplicationName() + "</name>");
            w.println("    <description>Spring Template</description>");
            w.println("    <url/>");
            w.println("    <licenses>");
            w.println("        <license/>");
            w.println("    </licenses>");
            w.println("    <developers>");
            w.println("        <developer/>");
            w.println("    </developers>");
            w.println("    <scm>");
            w.println("        <connection/>");
            w.println("        <developerConnection/>");
            w.println("        <tag/>");
            w.println("        <url/>");
            w.println("    </scm>");
            w.println();
            w.println("    <properties>");
            w.println("        <java.version>17</java.version>");
            w.println("    </properties>");
            w.println();
            w.println("    <dependencies>");
            // Spring Data JPA
            w.println("        <dependency>");
            w.println("            <groupId>org.springframework.boot</groupId>");
            w.println("            <artifactId>spring-boot-starter-data-jpa</artifactId>");
            w.println("        </dependency>");
            // Spring Web
            w.println("        <dependency>");
            w.println("            <groupId>org.springframework.boot</groupId>");
            w.println("            <artifactId>spring-boot-starter-webmvc</artifactId>");
            w.println("        </dependency>");
            // OpenAPI / Swagger UI em /swagger-ui.html (springdoc 3.x p/ Spring Boot 4) — opcional
            if (settings.isOpenApi()) {
                w.println("        <dependency>");
                w.println("            <groupId>org.springdoc</groupId>");
                w.println("            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>");
                w.println("            <version>3.0.0</version>");
                w.println("        </dependency>");
            }
            // Lombok
            w.println("        <dependency>");
            w.println("            <groupId>org.projectlombok</groupId>");
            w.println("            <artifactId>lombok</artifactId>");
            w.println("            <optional>true</optional>");
            w.println("        </dependency>");

            // Database dependency
            switch (settings.getDatabaseType()) {
                case H2:
                    w.println("        <dependency>");
                    w.println("            <groupId>com.h2database</groupId>");
                    w.println("            <artifactId>h2</artifactId>");
                    w.println("            <scope>runtime</scope>");
                    w.println("        </dependency>");
                    break;
                case POSTGRESQL:
                    w.println("        <dependency>");
                    w.println("            <groupId>org.postgresql</groupId>");
                    w.println("            <artifactId>postgresql</artifactId>");
                    w.println("            <scope>runtime</scope>");
                    w.println("        </dependency>");
                    break;
                case MYSQL:
                    w.println("        <dependency>");
                    w.println("            <groupId>com.mysql</groupId>");
                    w.println("            <artifactId>mysql-connector-j</artifactId>");
                    w.println("            <scope>runtime</scope>");
                    w.println("        </dependency>");
                    break;
            }

            // Test dependencies
            w.println("        <dependency>");
            w.println("            <groupId>org.springframework.boot</groupId>");
            w.println("            <artifactId>spring-boot-starter-data-jpa-test</artifactId>");
            w.println("            <scope>test</scope>");
            w.println("        </dependency>");
            w.println("        <dependency>");
            w.println("            <groupId>org.springframework.boot</groupId>");
            w.println("            <artifactId>spring-boot-starter-webmvc-test</artifactId>");
            w.println("            <scope>test</scope>");
            w.println("        </dependency>");

            w.println("    </dependencies>");
            w.println();
            w.println("    <build>");
            w.println("        <plugins>");
            w.println("            <plugin>");
            w.println("                <groupId>org.apache.maven.plugins</groupId>");
            w.println("                <artifactId>maven-compiler-plugin</artifactId>");
            w.println("                <configuration>");
            w.println("                    <annotationProcessorPaths>");
            w.println("                        <path>");
            w.println("                            <groupId>org.projectlombok</groupId>");
            w.println("                            <artifactId>lombok</artifactId>");
            w.println("                        </path>");
            w.println("                    </annotationProcessorPaths>");
            w.println("                </configuration>");
            w.println("            </plugin>");
            w.println("            <plugin>");
            w.println("                <groupId>org.springframework.boot</groupId>");
            w.println("                <artifactId>spring-boot-maven-plugin</artifactId>");
            w.println("                <configuration>");
            w.println("                    <excludes>");
            w.println("                        <exclude>");
            w.println("                            <groupId>org.projectlombok</groupId>");
            w.println("                            <artifactId>lombok</artifactId>");
            w.println("                        </exclude>");
            w.println("                    </excludes>");
            w.println("                </configuration>");
            w.println("            </plugin>");
            w.println("        </plugins>");
            w.println("    </build>");
            w.println();
            w.println("</project>");
        }
    }

    private void generateApplicationProperties() throws IOException {
        String filename = outputDirectory + "/src/main/resources/application.properties";
        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("spring.application.name=" + settings.getApplicationName());
            w.println("server.port=" + settings.getServerPort());
            w.println();

            switch (settings.getDatabaseType()) {
                case H2:
                    w.println("spring.datasource.url=jdbc:h2:mem:testdb");
                    w.println("spring.datasource.driver-class-name=org.h2.Driver");
                    w.println("spring.datasource.username=sa");
                    w.println("spring.datasource.password=");
                    w.println("spring.h2.console.enabled=true");
                    w.println("spring.h2.console.path=/h2-console");
                    w.println("spring.jpa.hibernate.ddl-auto=create-drop");
                    break;
                case POSTGRESQL:
                    w.println("spring.datasource.url=jdbc:postgresql://localhost:5432/" + settings.getApplicationName());
                    w.println("spring.datasource.driver-class-name=org.postgresql.Driver");
                    w.println("spring.datasource.username=postgres");
                    w.println("spring.datasource.password=postgres");
                    w.println("spring.jpa.hibernate.ddl-auto=update");
                    break;
                case MYSQL:
                    w.println("spring.datasource.url=jdbc:mysql://localhost:3306/" + settings.getApplicationName());
                    w.println("spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
                    w.println("spring.datasource.username=root");
                    w.println("spring.datasource.password=root");
                    w.println("spring.jpa.hibernate.ddl-auto=update");
                    break;
            }

            w.println();
            w.println("spring.jpa.show-sql=true");
            w.println("spring.jpa.properties.hibernate.format_sql=true");
            w.println();
            w.println("logging.level.org.springframework=INFO");
            w.println("logging.level." + settings.getBasePackage() + "=DEBUG");
        }
    }

    private void generateMainClass() throws IOException {
        String packagePath = settings.getPackagePath();
        String className = settings.getMainClassName();
        String filename = outputDirectory + "/src/main/java/" + packagePath + "/" + className + ".java";

        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("package " + settings.getBasePackage() + ";");
            w.println();
            w.println("import org.springframework.boot.SpringApplication;");
            w.println("import org.springframework.boot.autoconfigure.SpringBootApplication;");
            w.println();
            w.println("@SpringBootApplication");
            w.println("public class " + className + " {");
            w.println();
            w.println("    public static void main(String[] args) {");
            w.println("        SpringApplication.run(" + className + ".class, args);");
            w.println("    }");
            w.println("}");
        }
    }

    private void generateTestClass() throws IOException {
        String packagePath = settings.getPackagePath();
        String className = settings.getMainClassName() + "Tests";
        String filename = outputDirectory + "/src/test/java/" + packagePath + "/" + className + ".java";

        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("package " + settings.getBasePackage() + ";");
            w.println();
            w.println("import org.springframework.boot.test.context.SpringBootTest;");
            w.println("import org.junit.jupiter.api.Test;");
            w.println();
            w.println("@SpringBootTest");
            w.println("class " + className + " {");
            w.println();
            w.println("    @Test");
            w.println("    void contextLoads() {");
            w.println("    }");
            w.println("}");
        }
    }

}
