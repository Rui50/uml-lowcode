package service;

import generator.CLI_JPA.*;
import generator.GeneratorUtils;
import model.ParsedModel;
import model.ProjectSettings;
import model.Class;
import model.Association;
import model.Attribute;
import model.Operation;
import model.annotations.Invariant;
import model.annotations.Precondition;
import model.annotations.Postcondition;
import generator.CLI.JavaProjectGenerator;
import generator.CLI.JavaEntityGenerator;
import generator.plain.CodeGenerator;
import generator.plain.ClassGenerator;
import generator.spring.*;

public class GenerationService {
    public void generate(ParsedModel model, String target, String outputDir, ProjectSettings opts) throws Exception {
        validateSelfAssociations(model);
        switch (target) {
            case "plain"  -> generatePlain(model, outputDir, opts);
            case "cli"    -> generateCli(model, outputDir);
            case "spring" -> generateSpring(model, outputDir, opts);
            case "cli-jpa" -> generateCliJpa(model, outputDir, opts);

            default -> throw new IllegalArgumentException("unknown target: " + target);
        }
    }

    /**
     * numa auto associação as parters navegaveis precisa de roles diferentes,
     * Sem isso, o nome dos campo/variável fica no nome da classe e colide
     */
    private void validateSelfAssociations(ParsedModel model) {
        if (model.getAssociations() == null) return;
        for (Association a : model.getAssociations()) {
            if (a.getSourceClass() == null || !a.getSourceClass().equals(a.getTargetClass())) continue;
            boolean srcMissing = a.isSourceNavigable() && isBlank(a.getSourceRole());
            boolean tgtMissing = a.isTargetNavigable() && isBlank(a.getTargetRole());
            boolean sameRole = a.isSourceNavigable() && a.isTargetNavigable()
                    && !isBlank(a.getSourceRole()) && a.getSourceRole().equals(a.getTargetRole());
            if (srcMissing || tgtMissing || sameRole) {
                throw new IllegalArgumentException(
                        "Auto-associação em '" + a.getSourceClass() + "': cada ponta navegável precisa de um "
                        + "role distinto (ex: manager / subordinates). Sem roles distintos, os campos e "
                        + "parâmetros gerados colidem (a classe é os dois lados da associação).");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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
        // mapa nome-de-DTO -> classe dona: cada service/controller importa só as folders de DTO que usa
        java.util.Map<String, String> DTO_ToClass = SpringUtils.buildDTOOwnerMap(m.getClasses());
        new SpringServiceGenerator(outputDir, basePackage, DTO_ToClass)
                .generateServices(m.getClasses(), m.getAssociations(), /*m.getPsmInteractions()*/m.getAllInteractions(), m.getEnumerations());
        new SpringControllerGenerator(outputDir, basePackage, DTO_ToClass)
                .generateControllers(m.getClasses(), m.getAssociations(), m.getAllInteractions());//m.getPsmInteractions());
        new SpringJobGenerator(outputDir, basePackage)
                .generateJobs(m.getClasses(), m.getAssociations(), m.getEnumerations());

        // demo é opt (default false)
        if (settings.isDemoFrontend()) {
            new generator.spring.SpringFrontendGenerator(outputDir)
                    .generateFrontend(m.getClasses(), m.getAssociations(), m.getEnumerations());
        }
    }
}

