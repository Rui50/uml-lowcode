package service;

import generator.CLI_JPA.*;
import model.ParsedModel;
import model.ProjectSettings;
import parser.XMIParser;
import generator.CLI.JavaProjectGenerator;
import generator.CLI.JavaEntityGenerator;
import generator.plain.CodeGenerator;
import generator.plain.ClassGenerator;
import generator.spring.*;

public class GenerationService {
    public void generate(ParsedModel model, String target, String outputDir, ProjectSettings opts) throws Exception {
        switch (target) {
            case "plain"  -> generatePlain(model, outputDir, opts);
            case "cli"    -> generateCli(model, outputDir);
            case "spring" -> generateSpring(model, outputDir, opts);
            case "cli-jpa" -> generateCliJpa(model, outputDir, opts);

            default -> throw new IllegalArgumentException("unknown target: " + target);
        }
    }

    private void generatePlain(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        // plain não passa pelo modal de settings; usa o basePackage se vier, senão "model"
        String basePackage = (opts != null && opts.getBasePackage() != null && !opts.getBasePackage().isBlank())
                ? opts.getBasePackage() : "model";

        new CodeGenerator(
                m.getClasses(),
                m.getAssociations(),
                m.getEnumerations(),
                outputDir,
                basePackage,
                //m.getPimInteractions()
                m.getAllInteractions()
        ).generate();
        new ClassGenerator(outputDir, basePackage, m.getAllInteractions())
                .generateInterfaces(m.getInterfaces());
    }

    private void generateCli(ParsedModel m, String outputDir) throws Exception {
        new JavaProjectGenerator(outputDir).generate(
                m.getClasses(),
                m.getAssociations(),
                m.getEnumerations(),
                //m.getPimInteractions()
                m.getAllInteractions()
        );
        new JavaEntityGenerator(outputDir, m.getAllInteractions())
                .generateInterfaces(m.getInterfaces());
    }

    private void generateCliJpa(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        CLIProjectSettings settings = new CLIProjectSettings();
        if (opts != null) {
            if (opts.getBasePackage() != null && !opts.getBasePackage().isBlank()) {
                settings.setBasePackage(opts.getBasePackage());
            }
            if (opts.getApplicationName() != null && !opts.getApplicationName().isBlank()) {
                settings.setApplicationName(opts.getApplicationName());
            }
            if (opts.getDatabaseType() != null && !opts.getDatabaseType().isBlank()) {
                settings.setDatabaseType(generator.CLI_JPA.DatabaseType.valueOf(opts.getDatabaseType()));
            }
        }
        CLIProjectGenerator project = new CLIProjectGenerator(settings, outputDir);
        project.generateProjectStructure();

        String basePackage = settings.getBasePackage();

        new CLIEntityGenerator(outputDir, basePackage)
                .generateEntities(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new CLIEntityGenerator(outputDir, basePackage)
                .generateInterfaces(m.getInterfaces());
        new CLIRepositoryGenerator(outputDir, basePackage)
                .generateRepositories(m.getClasses());
        new CLIMenuGenerator(outputDir, basePackage, m.getAllInteractions())
                .generateMenus(m.getClasses(), m.getAssociations(), m.getEnumerations());
    }

    private void generateSpring(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        SpringProjectSettings settings = new SpringProjectSettings();
        if (opts != null) {
            if (opts.getBasePackage() != null && !opts.getBasePackage().isBlank()) {
                settings.setBasePackage(opts.getBasePackage());
            }
            if (opts.getApplicationName() != null && !opts.getApplicationName().isBlank()) {
                settings.setApplicationName(opts.getApplicationName());
            }
            if (opts.getDatabaseType() != null && !opts.getDatabaseType().isBlank()) {
                settings.setDatabaseType(generator.spring.DatabaseType.valueOf(opts.getDatabaseType()));
            }
            if (opts.getServerPort() != null) {
                settings.setServerPort(opts.getServerPort());
            }
        }
        SpringProjectGenerator project = new SpringProjectGenerator(settings, outputDir);
        project.generateProjectStructure();

        String basePackage = settings.getBasePackage();

        new SpringEntityGenerator(outputDir, basePackage)
                .generateEntities(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringEntityGenerator(outputDir, basePackage)
                .generateInterfaces(m.getInterfaces());
        new SpringDTOGenerator(outputDir, basePackage)
                .generateDTOs(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringRepositoryGenerator(outputDir, basePackage)
                .generateRepositories(m.getClasses());
        new SpringServiceGenerator(outputDir, basePackage)
                .generateServices(m.getClasses(), m.getAssociations(), /*m.getPsmInteractions()*/m.getAllInteractions(), m.getEnumerations());
        new SpringControllerGenerator(outputDir, basePackage)
                .generateControllers(m.getClasses(), m.getAssociations(), m.getAllInteractions());//m.getPsmInteractions());
        new SpringJobGenerator(outputDir, basePackage)
                .generateJobs(m.getClasses(), m.getAssociations(), m.getEnumerations());

    }
}

