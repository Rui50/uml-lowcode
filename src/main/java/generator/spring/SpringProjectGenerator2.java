package generator.spring;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SpringProjectGenerator2 {
    private String templateDirectory;
    private String outputDirectory;

    public SpringProjectGenerator2(String templateDirectory, String outputDirectory) {
        this.templateDirectory = templateDirectory;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Copia template para output
     * */
    public void generateProjectStructure() throws IOException {
        System.out.println("Generating Spring Boot project from template in: " + templateDirectory);

        File template = new File(templateDirectory);
        if (!template.exists() || !template.isDirectory()) {
            System.err.println("Template directory does not exist: " + templateDirectory);
            return;
        }

        File output = new File(outputDirectory);
        if (output.exists()) {
            deleteDirectory(output);
        }

        copyDirectory(template.toPath(), output.toPath());
        System.out.println("Project structure generated in: " + outputDirectory);

    }

    /**
     * copia dir recursivamente
     * */
    private void copyDirectory(Path source, Path target) throws IOException {
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

    /**
    * Deleta dir recursivamente, para ja apaga para ser mais facil seguir mudanças
    * */
    private void deleteDirectory(File directory) throws IOException {
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
