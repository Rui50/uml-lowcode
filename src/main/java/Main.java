import parser.XMIParser;

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
        System.out.println("Parsing associations...");
        List<model.Association> associations = parser.getAssociations();
        System.out.println("Associations extracted: " + associations.size());

        if (associations.isEmpty()) {
            System.out.println("No associations found");
        } else {
            for (model.Association a : associations) {
                System.out.println(a);
            }
        }
    }
}
