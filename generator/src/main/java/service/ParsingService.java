package service;

import model.ParsedModel;
import model.sequence.Interaction;
import parser.XMIParser;

import java.util.List;

public class ParsingService {
    public ParsedModel parse(String xmiFilePath) throws Exception {
        XMIParser parser = new XMIParser();
        parser.loadModel(xmiFilePath);

        List<Interaction> all = parser.getInteractions();

        ParsedModel model = new ParsedModel(
            parser.getClasses(),
            parser.getAssociations(),
            parser.getGeneralizations(),
            parser.getEnumerations(),
            all,
            parser.getInterfaces()
        );

        // validações do modelo, aqui só avisam (o modelo ainda carrega)
        // o generate depois bloqueia os erros que sao mais importantes
        model.getWarnings().addAll(ModelValidator.collectTypeWarnings(model.getClasses(), model.getEnumerations(), model.getInterfaces()));
        model.getWarnings().addAll(ModelValidator.findSelfAssociationErrors(model.getAssociations()));
        model.getWarnings().addAll(ModelValidator.findRelationFieldCollisions(model.getAssociations()));
        model.getWarnings().addAll(ModelValidator.findOperationNameCollisions(model.getClasses()));

        return model;
    }
}
