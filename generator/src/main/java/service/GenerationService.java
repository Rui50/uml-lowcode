package service;

import generator.CLI_JPA.*;
import java.util.List;
import java.util.Map;
import model.ParsedModel;
import model.ProjectSettings;
import generator.CLI.JavaProjectGenerator;
import generator.CLI.JavaEntityGenerator;
import generator.plain.CodeGenerator;
import generator.plain.ClassGenerator;
import generator.spring.*;

public class GenerationService {
    public void generate(ParsedModel model, String target, String outputDir, ProjectSettings opts) throws Exception {
        // erros que iam impedir compilação
        validateSelfAssociations(model);
        validateRelationFieldCollisions(model);
        switch (target) {
            case "plain"  -> generatePlain(model, outputDir, opts);
            case "cli"    -> generateCli(model, outputDir, opts);
            case "spring" -> generateSpring(model, outputDir, opts);
            case "cli-jpa" -> generateCliJpa(model, outputDir, opts);

            default -> throw new IllegalArgumentException("unknown target: " + target);
        }
    }

    // erros  (código gerado não compilaria), no parse vão como avisos (não bloqueiam o load)
    private void validateSelfAssociations(ParsedModel model) {
        List<String> errors = ModelValidator.findSelfAssociationErrors(model.getAssociations());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0));
        }
    }

    private void validateRelationFieldCollisions(ParsedModel model) {
        List<String> collisions = ModelValidator.findRelationFieldCollisions(model.getAssociations());
        if (!collisions.isEmpty()) {
            throw new IllegalArgumentException(collisions.get(0));
        }
    }

    private void generatePlain(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        String basePackage;
        if (opts != null && opts.getBasePackage() != null) {
            basePackage = opts.getBasePackage();
        } else {
            basePackage = "model";
        }

        new CodeGenerator(
                m.getClasses(),
                m.getAssociations(),
                m.getEnumerations(),
                outputDir,
                basePackage
        ).generate();
        new ClassGenerator(outputDir, basePackage)
                .generateInterfaces(m.getInterfaces());
    }

    private void generateCli(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        String basePackage;
        if (opts != null && opts.getBasePackage() != null) {
            basePackage = opts.getBasePackage();
        } else {
            basePackage = "app";
        }
        new JavaProjectGenerator(outputDir, basePackage).generate(
                m.getClasses(),
                m.getAssociations(),
                m.getEnumerations()
        );
        new JavaEntityGenerator(outputDir, basePackage)
                .generateInterfaces(m.getInterfaces());
    }

    private void generateCliJpa(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        CLIProjectSettings settings = new CLIProjectSettings();
        if (opts != null) {
            if (opts.getBasePackage() != null) {
                settings.setBasePackage(opts.getBasePackage());
            }
            if (opts.getApplicationName() != null) {
                settings.setApplicationName(opts.getApplicationName());
            }
            if (opts.getDatabaseType() != null) {
                settings.setDatabaseType(generator.CLI_JPA.DatabaseType.valueOf(opts.getDatabaseType()));
            }
            if (opts.getDockerfile() != null) {
                settings.setDockerfile(opts.getDockerfile());
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
        new CLIMenuGenerator(outputDir, basePackage)
                .generateMenus(m.getClasses(), m.getAssociations(), m.getEnumerations());
    }

    private void generateSpring(ParsedModel m, String outputDir, ProjectSettings opts) throws Exception {
        SpringProjectSettings settings = new SpringProjectSettings();
        if (opts != null) {
            if (opts.getBasePackage() != null) {
                settings.setBasePackage(opts.getBasePackage());
            }
            if (opts.getApplicationName() != null) {
                settings.setApplicationName(opts.getApplicationName());
            }
            if (opts.getDatabaseType() != null) {
                settings.setDatabaseType(generator.spring.DatabaseType.valueOf(opts.getDatabaseType()));
            }
            if (opts.getServerPort() != null) {
                settings.setServerPort(opts.getServerPort());
            }
            if (opts.getOpenApi() != null) {
                settings.setOpenApi(opts.getOpenApi());
            }
            if (opts.getDockerfile() != null) {
                settings.setDockerfile(opts.getDockerfile());
            }
            if (opts.getTimestamps() != null) {
                settings.setTimestamps(opts.getTimestamps());
            }
            if (opts.getDemoFrontend() != null) {
                settings.setDemoFrontend(opts.getDemoFrontend());
            }
        }
        SpringProjectGenerator project = new SpringProjectGenerator(settings, outputDir);
        project.generateProjectStructure();

        String basePackage = settings.getBasePackage();
        boolean timestamps = settings.isTimestamps();

        new SpringEntityGenerator(outputDir, basePackage, timestamps)
                .generateEntities(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringEntityGenerator(outputDir, basePackage, false)   // interfaces não estendem Auditable
                .generateInterfaces(m.getInterfaces());
        new SpringDTOGenerator(outputDir, basePackage, timestamps)
                .generateDTOs(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringRepositoryGenerator(outputDir, basePackage)
                .generateRepositories(m.getClasses());

        // mapa nome deDTO -> classe owner: cada service/controller importa só os folders de DTO que usa
        Map<String, String> DTO_ToClass = SpringUtils.buildDTOOwnerMap(m.getClasses());
        new SpringServiceGenerator(outputDir, basePackage, DTO_ToClass)
                .generateServices(m.getClasses(), m.getAssociations(), m.getEnumerations());
        new SpringControllerGenerator(outputDir, basePackage, DTO_ToClass)
                .generateControllers(m.getClasses(), m.getAssociations());
        new SpringJobGenerator(outputDir, basePackage)
                .generateJobs(m.getClasses(), m.getAssociations(), m.getEnumerations());

        // demo é opt (default false)
        if (settings.isDemoFrontend()) {
            new SpringFrontendGenerator(outputDir)
                    .generateFrontend(m.getClasses(), m.getAssociations(), m.getEnumerations());
        }
    }
}

