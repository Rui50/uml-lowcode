import generator.spring.SpringEntityGenerator;
import parser.XMIParser;
import generator.CodeGenerator;
import generator.spring.SpringProjectGenerator;

import java.util.List;

import model.Class;
import model.Association;
import model.Generalization;

import javax.swing.*;


/**
 * TODO
 * - fix em coisas como List<Aluno> porque fica como Teste.Aluno (nome do proj)
 * - decidir como fazer os maps, "Qualifiers?"
 */

public class Main {
    public static void main(String[] args) throws Exception {

        // File paths for the parser
        //String xmiFilePath = "test-models/test_classe_1_2.uml";
        String xmiFilePath = "test-models/aluno_turma_ocl.uml";


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

        // relações (ainda so associações)
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

        // heranças
        System.out.println("Parsing generalizations...");
        List<Generalization> generalizations = parser.getGeneralizations();
        System.out.println("Generalizations extracted: " + generalizations.size());

        if (generalizations.isEmpty()) {
            System.out.println("No generalizations found");
        } else {
            for (Generalization gen : generalizations) {
                System.out.println(gen);
            }
        }

        if (!generalizations.isEmpty()) {
            System.out.println("\nResolving inheritance references...");
            fillGeneralizations(classes, generalizations);
        }

        // gerar código
        String outputDir = "output";
        String templateDir = "templates/spring-boot-template";
        //CodeGenerator codeGenerator = new generator.CodeGenerator(classes, associations, outputDir);
        //codeGenerator.generate();

        SpringProjectGenerator project = new SpringProjectGenerator(
            templateDir,
            outputDir
        );

        project.generateProjectStructure();

        // gerar entidades
        SpringEntityGenerator entityGenerator = new SpringEntityGenerator(outputDir);
        entityGenerator.generateEntities(classes, associations);


    }


    // para ja fica aqui, depois meter num parser helper
    private static void fillGeneralizations(List<Class> classes, List<Generalization> generalizations){
        for (Generalization gen : generalizations) {
            Class child = findClassByName(classes, gen.getChildClass());
            Class parent = findClassByName(classes, gen.getParentClass());

            if (child != null && parent != null) {
                child.setParentClass(parent);
            }
        }
    }

    private static Class findClassByName(List<Class> classes, String name) {
        for (Class c : classes) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
}
