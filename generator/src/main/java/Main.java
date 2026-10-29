import model.*;

import service.GenerationService;
import service.ParsingService;

public class Main {
    public static void main(String[] args) throws Exception {
        // para testes diretos
        String xmiFilePath = "test-models/library.uml";
        String outputDir = "output";
        String target = "spring";

        ParsedModel model = new ParsingService().parse(xmiFilePath);
        new GenerationService().generate(model, target, outputDir, null);
    }
}
