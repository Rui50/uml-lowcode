package generator;

import model.Class;
import model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

// use templates later

public class CodeGenerator {
    private List<Class> classes;
    private List <Association> associations;

    private String outputDirectory;

    private ClassGenerator classGenerator;

    public CodeGenerator(List<Class> classes, List<Association> associations, String outputDirectory) {
        this.classes = classes;
        this.associations = associations;
        this.outputDirectory = outputDirectory;

        this.classGenerator = new ClassGenerator(outputDirectory);
    }

    public void generate() throws IOException{
        // cria pasta output
        File dir = new File(outputDirectory);
        if (!dir.exists()){
            dir.mkdirs();
        }

        System.out.println("Generating code in: " + outputDirectory);

        // gerar código para cada classe
        System.out.println("Generating class structures...");
        classGenerator.generateClasses(classes, associations);

        System.out.println("Generation completed");
    }
}
