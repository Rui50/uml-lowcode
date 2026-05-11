package service;

import generator.CLI_JPA.*;
import model.ParsedModel;
import parser.XMIParser;
import generator.CLI.JavaProjectGenerator;
import generator.plain.CodeGenerator;
import generator.spring.*;
import model.ParsedModel;

public class GenerationService {
    public void generate(ParsedModel model, String target, String outputDir) throws Exception {
        switch (target) {
            case "plain"  -> generatePlain(model, outputDir);
            case "cli"    -> generateCli(model, outputDir);
            case "spring" -> generateSpring(model, outputDir);
            case "cli-jpa" -> generateCliJpa(model, outputDir);

            default -> throw new IllegalArgumentException("unknown target: " + target);
        }
    }

    private void generatePlain(ParsedModel m, String outputDir) throws Exception {
        new CodeGenerator(
                m.getClasses(),
                m.getAssociations(),
                m.getEnumerations(),
                outputDir,
                //m.getPimInteractions()
                m.getAllInteractions()
        ).generate();
    }

    private void generateCli(ParsedModel m, String outputDir) throws Exception {
        new JavaProjectGenerator(outputDir).generate(
                m.getClasses(),
                m.getAssociations(),
                m.getEnumerations(),
                //m.getPimInteractions()
                m.getAllInteractions()
        );
    }

    private void generateCliJpa(ParsedModel m, String outputDir) throws Exception {
        CLIProjectSettings settings = new CLIProjectSettings();
        CLIProjectGenerator project = new CLIProjectGenerator(settings, outputDir);
        project.generateProjectStructure();

        String basePackage = settings.getBasePackage();

        new CLIEntityGenerator(outputDir, basePackage)
                .generateEntities(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new CLIRepositoryGenerator(outputDir, basePackage)
                .generateRepositories(m.getClasses());
        new CLIMenuGenerator(outputDir, basePackage, m.getAllInteractions())
                .generateMenus(m.getClasses(), m.getAssociations(), m.getEnumerations());
    }

    private void generateSpring(ParsedModel m, String outputDir) throws Exception {
        SpringProjectSettings settings = new SpringProjectSettings();
        SpringProjectGenerator project = new SpringProjectGenerator(settings, outputDir);
        project.generateProjectStructure();

        String basePackage = settings.getBasePackage();

        new SpringEntityGenerator(outputDir, basePackage)
                .generateEntities(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringDTOGenerator(outputDir, basePackage)
                .generateDTOs(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringRepositoryGenerator(outputDir, basePackage)
                .generateRepositories(m.getClasses());
        new SpringServiceGenerator(outputDir, basePackage)
                .generateServices(m.getClasses(), m.getAssociations(), /*m.getPsmInteractions()*/m.getAllInteractions(), m.getEnumerations());
        new SpringControllerGenerator(outputDir, basePackage)
                .generateControllers(m.getClasses(), m.getAssociations(), m.getAllInteractions());//m.getPsmInteractions());

    }
}

