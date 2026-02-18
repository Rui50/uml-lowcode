import model.Association;
import parser.XMIParser;
import generator.CodeGenerator;

import java.util.List;

import model.Class;

public class Main {
    public static void main(String[] args) throws Exception {

        // File paths for the parser
        String xmiFilePath = "test-models/test_classe_1_2.uml";

        System.out.println("Parsing UML model from: " + xmiFilePath);

        // Parsing
        XMIParser parser = new XMIParser();
        System.out.println("Loading model...");
        parser.loadModel(xmiFilePath);

        // Extrair classes
        System.out.println("Extracting classes...");
        List<Class> classes = parser.getClasses();

        System.out.println("Parsing completed successfully!");

        // quantas classes
        System.out.println("Classes extracted: "  + classes.size());

        // detalhes
        if (classes.isEmpty()) {
            System.out.println("No classes found");
        } else {
            for (Class c : classes) {
                System.out.println(c);
            }
        }

        // relações
        System.out.println("Parsing relationships...");
        List<Association> associations = parser.getAssociations();
        System.out.println("Relationships extracted: " + associations.size());

        if (associations.isEmpty()) {
            System.out.println("No relationships found");
        } else {
            for (Association as : associations) {
                System.out.println(as);
            }
        }


        // gerar código
        String outputDir = "output";
        CodeGenerator codeGenerator = new generator.CodeGenerator(classes, associations, outputDir);
        codeGenerator.generate();
    }
}
