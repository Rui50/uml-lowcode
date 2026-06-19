import model.*;

import service.GenerationService;
import service.ParsingService;

public class Main {
    public static void main(String[] args) throws Exception {
        //String xmiFilePath = "test-models/interface.uml";
        String xmiFilePath = "test-models/interface.uml";
        String outputDir = "output";
        String target = "cli-jpa";

        ParsedModel model = new ParsingService().parse(xmiFilePath);
        new GenerationService().generate(model, target, outputDir, null);

    }

        /*String xmiFilePath = "test-models/interface.uml";

        System.out.println("Parsing UML model from: " + xmiFilePath);

        // Parsing do diagrama de classes
        XMIParser parser = new XMIParser();
        parser.loadModel(xmiFilePath);

        // Extrair classes
        System.out.println("Extracting classes...");
        List<Class> classes = parser.getClasses();
        // detalhes
        if (classes.isEmpty()) {
            System.out.println("No classes found");
        } else {
            for (Class c : classes) {
                System.out.println(c);
            }
        }

        // relações
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

        // Parsing das enumerações
        List<Enumeration> enumerations = parser.getEnumerations();
        System.out.println("Enumerations extracted: " + enumerations.size());
        if (enumerations.isEmpty()) {
            System.out.println("No enumerations found");
        } else {
            for (Enumeration e : enumerations) {
                System.out.println(e);
            }
        }

        List<Interaction> allInteractions = parser.getInteractions();

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

        // debug: imprimir interactions parseadas
        for (Interaction inter : allInteractions) {
            System.out.println("\n=== Interaction: " + inter.getName() + " ===");
            System.out.println("Lifelines: " + inter.getLifelines());
            System.out.println("Elements:");
            printElements(inter.getElements(), 1);
        }

        String outputDir = "output";
        String generateWhat = "spring"; // "plain" "spring" "cli" "none"

        switch (generateWhat) {
            case "plain"  -> generatePlain(classes, associations, enumerations, pimInteractions, outputDir);
            case "spring" -> generateSpring(classes, associations, enumerations, psmInteractions, outputDir);
            case "cli"    -> generateCli(classes, associations, enumerations, pimInteractions, outputDir);
        }

        printOclTranslations(classes);
    }

    private static void generatePlain(List<Class> classes, List<Association> associations,
                                      List<Enumeration> enumerations, List<Interaction> interactions,
                                      String outputDir) throws Exception {
        CodeGenerator codeGenerator = new CodeGenerator(classes, associations, enumerations, outputDir, interactions);
        codeGenerator.generate();
    }

    private static void generateSpring(List<Class> classes, List<Association> associations,
                                       List<Enumeration> enumerations, List<Interaction> interactions,
                                       String outputDir) throws Exception {
        SpringProjectSettings settings = new SpringProjectSettings();
        // settings.setBasePackage("pt.uminho.app");
        // settings.setDatabaseType(DatabaseType.POSTGRESQL);
        // settings.setApplicationName("my-app");
        // settings.setServerPort(9090);

        SpringProjectGenerator project = new SpringProjectGenerator(settings, outputDir);
        project.generateProjectStructure();

        String basePackage = settings.getBasePackage();

        new SpringEntityGenerator(outputDir, basePackage).generateEntities(classes, associations, enumerations);
        new SpringDTOGenerator(outputDir, basePackage).generateDTOs(classes, associations, enumerations);
        new SpringRepositoryGenerator(outputDir, basePackage).generateRepositories(classes);
        new SpringServiceGenerator(outputDir, basePackage).generateServices(classes, associations, interactions, enumerations);
        new SpringControllerGenerator(outputDir, basePackage).generateControllers(classes, associations, interactions);
    }

    private static void generateCli(List<Class> classes, List<Association> associations,
                                    List<Enumeration> enumerations, List<Interaction> interactions,
                                    String outputDir) throws Exception {
        new JavaProjectGenerator(outputDir).generate(classes, associations, enumerations, interactions);
    }

    private static void printOclTranslations(List<Class> classes) {
        for (Class c : classes) {
            for (Invariant i : c.getInvariants()) {
                System.out.println("OCL:   " + i.getOclExpression());
                System.out.println("ANTLR: " + translateOcl(i.getOclExpression()));
            }
            for (Attribute a : c.getAttributes()) {
                if (a.getInitExpression() != null) {
                    System.out.println("OCL:   " + a.getInitExpression());
                    System.out.println("ANTLR: " + translateOcl(a.getInitExpression()));
                }
            }
        }
    }

    private static String translateOcl(String expr) {
        OCLLexer lexer = new OCLLexer(CharStreams.fromString(expr));
        OCLParser p = new OCLParser(new CommonTokenStream(lexer));
        return new OCLToJavaVisitor().visit(p.standaloneExpression());
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
    }*/
}
