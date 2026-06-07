import model.ParsedModel;
import service.GenerationService;
import service.ParsingService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test: confirma que a geração corre SEM EXCEÇÃO para os 4 targets e
 * produz ficheiros .java. Não compila o output (ver smoke.sh para isso) —
 * é uma rede de segurança rápida contra crashes/regressões do gerador.
 */
public class GenerationSmokeTest {

    private static final String MODEL = "test-models/aluno_turma_ocl_fix.uml";

    @Test void plain()  throws Exception { generates("plain"); }
    @Test void cli()    throws Exception { generates("cli"); }
    @Test void cliJpa() throws Exception { generates("cli-jpa"); }
    @Test void spring() throws Exception { generates("spring"); }

    private void generates(String target) throws Exception {
        Path out = Files.createTempDirectory("smoke-" + target + "-");
        ParsedModel model = new ParsingService().parse(MODEL);

        // não deve lançar exceção
        assertDoesNotThrow(
                () -> new GenerationService().generate(model, target, out.toString(), null),
                "geração lançou exceção para target '" + target + "'");

        // e deve ter produzido pelo menos um .java
        try (Stream<Path> s = Files.walk(out)) {
            assertTrue(s.anyMatch(p -> p.toString().endsWith(".java")),
                    "target '" + target + "' não gerou nenhum .java");
        }
    }
}
