package generator;

import model.Class;
import model.Enumeration;
import model.*;
import model.sequence.Interaction;

import java.io.File;
import java.io.IOException;
import java.util.List;

// use templates later

public class CodeGenerator {
    private List<Class> classes;
    private List <Association> associations;
    private List<Enumeration> enumerations;
    private List<Interaction> interactions;

    private String outputDirectory;

    private ClassGenerator classGenerator;

    public CodeGenerator(List<Class> classes, List<Association> associations, List<Enumeration> enumerations, String outputDirectory, List<Interaction> interactions) {
        this.classes = classes;
        this.associations = associations;
        this.enumerations = enumerations;
        this.outputDirectory = outputDirectory;
        this.interactions = interactions;

        this.classGenerator = new ClassGenerator(outputDirectory, interactions);
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
        classGenerator.generateClasses(classes, associations, enumerations);

        System.out.println("Generation completed");
    }
}
