package parser;
// https://www.sdmetrics.com/javadoc/com/sdmetrics/model/package-summary.html

import java.util.*;

import com.sdmetrics.model.MetaModel;
import com.sdmetrics.model.Model;
import com.sdmetrics.model.XMIReader;
import com.sdmetrics.model.XMITransformations;
import com.sdmetrics.util.XMLParser;

import model.*;

import model.Class;
import model.Enumeration;
import model.sequence.Interaction;
import parser.VpParser.Qualifier;

public class XMIParser {
    // coisas do sdmetrics
    private Model model;
    private MetaModel metaModel;

    // qualifiers do VP tem o seu proprio parsing (fora do sdmetrics)
    private Map<String, Qualifier> qualifiers;

    private List<Class> classes;
    private List<Association> associations;
    private List<Generalization> generalizations;
    private List<Enumeration> enumerations;
    private List<Interaction> interactions;
    private List<Interface> interfaces;

    public void loadModel(String xmiFile) throws Exception{

        // caminhos dos ficheiros de configuração do SDMetrics (para uml 2.0)
        // carregados do /resources
        String metaModelURL  = getClass().getResource("/metamodel2.xml").toString();
        String xmiTransURL = getClass().getResource("/xmiTrans2_0.xml").toString();

        // parser do SDMetrics
        XMLParser parser = new XMLParser();

        // carregar o metamodelo UML
        System.out.println("Loading meta-model from: " + metaModelURL );
        metaModel = new MetaModel();
        parser.parse(metaModelURL , metaModel.getSAXParserHandler());

        // carregar as trasnformações
        System.out.println("Loading transformations from: " + xmiTransURL);
        XMITransformations trans=new XMITransformations(metaModel);
        parser.parse(xmiTransURL, trans.getSAXParserHandler());

        // ler o ficheiro XMI com o modelo UML
        System.out.println("Loading model from: " + xmiFile);
        model = new Model(metaModel);
        XMIReader xmiReader = new XMIReader(trans, model);
        parser.parse(xmiFile, xmiReader);

        System.out.println("Model loaded");

        // parsing manual dos qualifiers ( mapas)
        qualifiers = VpParser.parseQualifiers(xmiFile);

        // class diagram
        ClassDiagramParser cdp = new ClassDiagramParser(
                model, metaModel, qualifiers
        );
        classes = cdp.parseClasses();
        associations = cdp.parseAssociations();
        generalizations = cdp.parseGeneralizations();
        enumerations = cdp.parseEnumerations();
        interfaces = cdp.parseInterfaces();

        cdp.fillGeneralizations(classes, generalizations);
        cdp.fillRealizations(classes, interfaces);

        // sequence diagrams
        SequenceDiagramParser sdp = new SequenceDiagramParser(model, metaModel, xmiFile);
        interactions = sdp.parseInteractions();
    }

    public List<Class> getClasses()                  { return classes; }
    public List<Association> getAssociations()       { return associations; }
    public List<Generalization> getGeneralizations() { return generalizations; }
    public List<Enumeration> getEnumerations()       { return enumerations; }
    public List<Interaction> getInteractions()       { return interactions; }
    public List<Interface> getInterfaces()              { return interfaces; }
}
