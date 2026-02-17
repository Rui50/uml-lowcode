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
        System.out.println("Parsing relationships...");
        List<model.Relationship> relationships = parser.getRelationships();
        System.out.println("Relationships extracted: " + relationships.size());

        if (relationships.isEmpty()) {
            System.out.println("No relationships found");
        } else {
            for (model.Relationship relationship : relationships) {
                System.out.println(relationship);
            }
        }
    }
}
