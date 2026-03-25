import model.annotations.Invariant;
import model.sequence.Interaction;
import ocl.OCLTranslator;
import parser.SequenceDiagramParser;
import parser.XMIParser;
import generator.CodeGenerator;
import generator.spring.SpringProjectGenerator;
import generator.spring.SpringEntityGenerator;
import generator.spring.SpringRepositoryGenerator;
import generator.spring.SpringServiceGenerator;
import generator.spring.SpringControllerGenerator;

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

        // paths for the parser
        //String xmiFilePath = "test-models/test_classe_1_2.uml";
        String xmiFilePath = "test-models/aluno_turma_ocl_fix.uml";
        //String xmiFilePath = "test-models/aluno_turma_class_sequence.uml";

        System.out.println("Parsing UML model from: " + xmiFilePath);

        // Parsing do diagrama de classes
        XMIParser parser = new XMIParser();
        System.out.println("Loading model...");
        parser.loadModel(xmiFilePath);

        // Extrair classes
        System.out.println("Extracting classes...");
        List<Class> classes = parser.getClasses();

        System.out.println("Parsing completed successfully!");

        // quantas classes para debug
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

        // heranças tratadas separadamente
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

        // Parsing dos diagramas de sequencia
        SequenceDiagramParser sequenceParser = new SequenceDiagramParser();
        sequenceParser.loadModel(xmiFilePath);
        List<Interaction> interactions = sequenceParser.parseInteractions();

        // gerar código
        String outputDir = "output";
        String templateDir = "templates/spring-boot-template-h2";
        // plain java classes
        CodeGenerator codeGenerator = new generator.CodeGenerator(classes, associations, outputDir);
        codeGenerator.generate();

        // spring boot
        SpringProjectGenerator project = new SpringProjectGenerator(
            templateDir,
            outputDir
        );

        project.generateProjectStructure();

        // gerar entidades
        SpringEntityGenerator entityGenerator = new SpringEntityGenerator(outputDir);
        entityGenerator.generateEntities(classes, associations);

        // gerar repositórios
        SpringRepositoryGenerator repositoryGenerator = new SpringRepositoryGenerator(outputDir);
        repositoryGenerator.generateRepositories(classes);

        // gerar serviços
        SpringServiceGenerator serviceGenerator = new SpringServiceGenerator(outputDir);
        serviceGenerator.generateServices(classes, associations, interactions);

        // gerar controllers
        SpringControllerGenerator controllerGenerator = new SpringControllerGenerator(outputDir);
        controllerGenerator.generateControllers(classes, associations, interactions);

        // ver as operações OCL (invariantes para ja)
        OCLTranslator translator = new OCLTranslator();
        for (Class c : classes) {
            for(Invariant i : c.getInvariants()){
                String java = translator.translateToJava(i.getOclExpression());
                System.out.println("OCL: " + i.getOclExpression());
                System.out.println("Java: " + java);
            }
        }
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
