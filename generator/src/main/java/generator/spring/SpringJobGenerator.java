package generator.spring;

import generator.GeneratorUtils;
import model.Association;
import model.Class;
import model.Operation;
import model.sequence.Interaction;
import model.sequence.Lifeline;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gera as classes de jobs (@Scheduled) e a config @EnableScheduling. Cada job é um @Component
 * que injeta os repositórios do seu diagrama e corre o corpo (do SequenceBodyGenerator).
 * Só gera a pasta etc se tiver pelos menos uma op marcada como job
 */
public class SpringJobGenerator {

    private final String baseOutputPath;
    private final String basePackage;

    public SpringJobGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    public void generateJobs(List<Class> classes, List<Association> associations,
                             List<model.Enumeration> enumerations) throws IOException {
        boolean anyJob = classes.stream()
                .flatMap(c -> c.getOperations().stream())
                .anyMatch(Operation::isJob);
        if (!anyJob) return;

        System.out.println("Generating Spring jobs...");
        String jobsDir = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/jobs";
        new java.io.File(jobsDir).mkdirs();

        generateSchedulingConfig(jobsDir);
        for (Class cls : classes) {
            for (Operation op : cls.getOperations()) {
                if (op.isJob()) generateJobClass(cls, op, classes, associations, enumerations, jobsDir);
            }
        }
        System.out.println("Job generation completed.");
    }

    // @Configuration @EnableScheduling, "Ligar" o scheduler do Spring
    private void generateSchedulingConfig(String jobsDir) throws IOException {
        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(jobsDir + "/SchedulingConfig.java"))) {
            writer.println("package " + basePackage + ".jobs;");
            writer.println();
            writer.println("import org.springframework.context.annotation.Configuration;");
            writer.println("import org.springframework.scheduling.annotation.EnableScheduling;");
            writer.println();
            writer.println("@Configuration");
            writer.println("@EnableScheduling");
            writer.println("public class SchedulingConfig {");
            writer.println("}");
        }
        System.out.println("Generated: SchedulingConfig.java");
    }

    // classe de job, @Component com os repos do diagrama + metodo @Scheduled
    private void generateJobClass(Class cls, Operation op, List<Class> classes, List<Association> associations,
                                  List<model.Enumeration> enumerations, String jobsDir) throws IOException {
        Interaction interaction = op.getSequenceDiagram();

        if (interaction == null) {
            throw new IllegalArgumentException("Job '" + op.getName()
                    + "': não tem diagrama de sequência associado — ligue um diagrama à operação ou desmarque-a como job.");
        }
        // prefixo da classe para evitar colisão entre jobs de classes diferentes
        String jobName = cls.getName() + GeneratorUtils.capitalizeFirst(op.getName()) + "Job";
        String cron = (op.getCron() != null && !op.getCron().isBlank()) ? op.getCron() : "0 * * * * *";

        // repos = lifelines do tipo *Repository desenhadas no diagrama do job
        Set<String> repoTypes = new LinkedHashSet<>();
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() == null || !ll.getType().endsWith("Repository")) continue;
            String entity = ll.getType().substring(0, ll.getType().length() - "Repository".length());
            boolean known = classes.stream().anyMatch(c -> c.getName().equals(entity));
            if (!known) {
                // lifeline de repo de classe inexistente
                throw new IllegalArgumentException("Job '" + op.getName()
                        + "': o diagrama tem uma lifeline ': " + ll.getType() + "' mas a classe '"
                        + entity + "' não existe no modelo.");
            }
            repoTypes.add(ll.getType());
        }

        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(jobsDir + "/" + jobName + ".java"))) {
            writer.println("package " + basePackage + ".jobs;");
            writer.println();
            for (Class c : classes) writer.println("import " + basePackage + ".models." + c.getName() + ";");
            for (model.Enumeration e : enumerations) writer.println("import " + basePackage + ".models." + e.getName() + ";");
            for (String repo : repoTypes) writer.println("import " + basePackage + ".repository." + repo + ";");
            writer.println();
            writer.println("import lombok.RequiredArgsConstructor;");
            writer.println("import org.springframework.stereotype.Component;");
            writer.println("import org.springframework.transaction.annotation.Transactional;");
            writer.println("import org.springframework.scheduling.annotation.Scheduled;");
            writer.println("import " + basePackage + ".exception.NotFoundException;");
            writer.println();
            writer.println("import java.util.*;");
            writer.println("import java.util.stream.Collectors;");
            writer.println();
            writer.println("@Component");
            writer.println("@RequiredArgsConstructor");
            writer.println("@Transactional");
            writer.println("public class " + jobName + " {");
            writer.println();
            for (String repo : repoTypes) {
                writer.println("    private final " + repo + " " + GeneratorUtils.lowercaseFirst(repo) + ";");
            }
            writer.println();
            writer.println("    @Scheduled(cron = \"" + cron + "\")");
            writer.println("    public void run() {");
            new SequenceBodyGenerator(associations).generateMethodBodyFromInteraction(writer, cls, op, interaction);
            writer.println("    }");
            writer.println("}");
        }
        System.out.println("Generated: " + jobName + ".java");
    }
}
