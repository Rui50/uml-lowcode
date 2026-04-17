import generator.OperationGenerator;
import model.Attribute;
import model.annotations.Invariant;
import model.sequence.*;;
import ocl.OCLTranslator;
import parser.SequenceDiagramParser;
import parser.SequenceDiagramParser2;
import parser.XMIParser;
import generator.CodeGenerator;
import generator.GeneratorUtils;
import generator.spring.SpringProjectGenerator;
import generator.spring.SpringEntityGenerator;
import generator.spring.SpringRepositoryGenerator;
import generator.spring.SpringServiceGenerator;
import generator.spring.SpringControllerGenerator;
import generator.spring.SpringDTOGenerator;
import generator.spring.SpringProjectSettings;

import generator.CLI.JavaProjectGenerator;

import java.io.PrintWriter;
import java.util.List;

import model.Class;
import model.Association;
import model.Enumeration;
import model.Generalization;

import javax.swing.*;

import ocl.OCLLexer;
import ocl.OCLParser;
import ocl.OCLToJavaVisitor;
import org.antlr.v4.runtime.*;
/**
 * TODO
 * - fix em coisas como List<Aluno> porque fica como Teste.Aluno (nome do proj)
 * - decidir como fazer os maps, "Qualifiers?"
 */

public class Main {
    public static void main(String[] args) throws Exception {

        // paths for the parser
        //String xmiFilePath = "test-models/test_classe_1_2.uml";
        //String xmiFilePath = "test-models/aluno_turma_ocl_fix.uml";
        //String xmiFilePath = "test-models/aluno_turma_class_sequence_test.uml";
        //String xmiFilePath = "test-models/aluno_turma_class_sequence_ocl_init.uml";
        String xmiFilePath = "test-models/aluno_turma_class_seq_full_test.uml";

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

        // Parsing das enumerações
        System.out.println("Parsing enumerations...");
        List<Enumeration> enumerations = parser.getEnumerations();
        System.out.println("Enumerations extracted: " + enumerations.size());

        if (enumerations.isEmpty()) {
            System.out.println("No enumerations found");
        } else {
            for (Enumeration e : enumerations) {
                System.out.println(e);
            }
        }

        // Parsing dos diagramas de sequencia
        SequenceDiagramParser sequenceParser = new SequenceDiagramParser();
        sequenceParser.loadModel(xmiFilePath);
        List<Interaction> allInteractions = sequenceParser.parseInteractions();

        // separar PSM (Spring) e PIM (CLI/plain)
        List<Interaction> psmInteractions = new java.util.ArrayList<>();
        List<Interaction> pimInteractions = new java.util.ArrayList<>();
        for (Interaction inter : allInteractions) {
            String name = inter.getName();
            if (name == null) continue;
            if (name.endsWith("_psm")) {
                inter.setName(name.substring(0, name.length() - 4));
                psmInteractions.add(inter);
            } else if (name.endsWith("_pim")) {
                inter.setName(name.substring(0, name.length() - 4));
                pimInteractions.add(inter);
            }
        }

        // por omissão o spring usa PSM; se não houver _psm cai para todas (compat com modelos antigos)
        // debug: imprimir interactions parseadas
        for (Interaction inter : allInteractions) {
            System.out.println("\n=== Interaction: " + inter.getName() + " ===");
            System.out.println("Lifelines: " + inter.getLifelines());
            System.out.println("Elements:");
            printElements(inter.getElements(), 1);
        }

        List<Interaction> interactions = null;

        // gerar código
        String outputDir = "output";

        String generateWhat = "cli"; // "plain" "spring" "cli" "none"

        // plain java classes
        if (generateWhat.equals("plain")) {
            interactions = pimInteractions;

            CodeGenerator codeGenerator = new CodeGenerator(classes, associations, enumerations, outputDir, pimInteractions);
            codeGenerator.generate();
        }

        if (generateWhat.equals("spring")) {
            interactions = psmInteractions;

            // spring boot - settings
            SpringProjectSettings settings = new SpringProjectSettings();
            // settings.setBasePackage("pt.uminho.app");
            // settings.setDatabaseType(DatabaseType.POSTGRESQL);
            // settings.setApplicationName("my-app");
            // settings.setServerPort(9090);

            SpringProjectGenerator project = new SpringProjectGenerator(settings, outputDir);
            project.generateProjectStructure();

            String basePackage = settings.getBasePackage();

            // gerar entidades
            SpringEntityGenerator entityGenerator = new SpringEntityGenerator(outputDir, basePackage);
            entityGenerator.generateEntities(classes, associations, enumerations);

            // gerar DTOs
            SpringDTOGenerator dtoGenerator = new SpringDTOGenerator(outputDir, basePackage);
            dtoGenerator.generateDTOs(classes, associations, enumerations);

            // gerar repositórios
            SpringRepositoryGenerator repositoryGenerator = new SpringRepositoryGenerator(outputDir, basePackage);
            repositoryGenerator.generateRepositories(classes);

            // gerar serviços
            SpringServiceGenerator serviceGenerator = new SpringServiceGenerator(outputDir, basePackage);
            serviceGenerator.generateServices(classes, associations, interactions, enumerations);

            // gerar controllers
            SpringControllerGenerator controllerGenerator = new SpringControllerGenerator(outputDir, basePackage);
            controllerGenerator.generateControllers(classes, associations, interactions);
        }

        if (generateWhat.equals("cli")) {
            interactions = pimInteractions;
            JavaProjectGenerator cliGenerator = new JavaProjectGenerator(outputDir);
            cliGenerator.generate(classes, associations, enumerations, interactions);
        }

        // ver as operações OCL (invariantes para ja)
        OCLTranslator translator = new OCLTranslator();
        for (Class c : classes) {
            for(Invariant i : c.getInvariants()){
                String java = translator.translateToJava(i.getOclExpression());
                System.out.println("OCL: " + i.getOclExpression());
                System.out.println("Java: " + java);
            }
        }

        for (Class c : classes) {
            for (Invariant i : c.getInvariants()) {
                OCLLexer lexer = new OCLLexer(CharStreams.fromString(i.getOclExpression()));
                OCLParser parser2 = new OCLParser(new CommonTokenStream(lexer));
                String java = new OCLToJavaVisitor().visit(parser2.standaloneExpression());
                System.out.println("OCL:   " + i.getOclExpression());
                System.out.println("ANTLR: " + java);
            }
        }

        for (Class c : classes) {
            for (Attribute a : c.getAttributes()) {
                if (a.getInitExpression() != null) {
                    OCLLexer lexer = new OCLLexer(CharStreams.fromString(a.getInitExpression()));
                    OCLParser parser2 = new OCLParser(new CommonTokenStream(lexer));
                    String java = new OCLToJavaVisitor().visit(parser2.standaloneExpression());
                    System.out.println("OCL:   " + a.getInitExpression());
                    System.out.println("ANTLR: " + java);
                }
            }
        }
    }

    // para ja fica aqui, depois meter num parser helper
    private static void fillGeneralizations(List<Class> classes, List<Generalization> generalizations){
        for (Generalization gen : generalizations) {
            Class child = GeneratorUtils.findClassByName(classes, gen.getChildClass());
            Class parent = GeneratorUtils.findClassByName(classes, gen.getParentClass());

            if (child != null && parent != null) {
                child.setParentClass(parent);
            }
        }
    }

    private static void printElements(java.util.List<Object> elements, int depth) {
        String indent = "  ".repeat(depth);
        for (Object el : elements) {
            if (el instanceof Message) {
                Message msg = (Message) el;
                System.out.println(indent + "Message: " + msg.getName()
                        + (msg.getAssignVariable() != null ? " (assign: " + msg.getAssignVariable() + ")" : "")
                        + " [" + (msg.getSource() != null ? msg.getSource().getType() : "?")
                        + " -> " + (msg.getTarget() != null ? msg.getTarget().getType() : "?") + "]"
                        + (msg.getReturnType() != null ? " : " + msg.getReturnType() : ""));
                if (msg.getArguments() != null && !msg.getArguments().isEmpty()) {
                    System.out.println(indent + "  args: " + msg.getArguments());
                }
            } else if (el instanceof Fragment) {
                Fragment frag = (Fragment) el;
                System.out.println(indent + "Fragment: " + frag.getOperator());
                for (Operand op : frag.getOperands()) {
                    System.out.println(indent + "  Operand [guard: " + op.getGuard() + "]");
                    printElements(op.getElements(), depth + 2);
                }
            }
        }
    }
}
