package parser;
// https://www.sdmetrics.com/javadoc/com/sdmetrics/model/package-summary.html

import java.util.*;

import com.sdmetrics.model.MetaModel;
import com.sdmetrics.model.Model;
import com.sdmetrics.model.ModelElement;
import com.sdmetrics.model.XMIReader;
import com.sdmetrics.model.XMITransformations;
import com.sdmetrics.util.XMLParser;

import model.*;

import model.Class;
import model.Enumeration;
import model.annotations.Invariant;
import model.sequence.Interaction;
import parser.VpParser.Qualifier;

public class XMIParser {
    // coisas do sdmetrics
    private Model model;
    private MetaModel metaModel;

    // qualifiers do VP tem o seu proprio parsing (fora do sdmetrics)
    private Map<String, Qualifier> qualifiers;

    // elementId da classe -> lista de invariantes
    private Map<String, List<Invariant>> invariantsCache = new HashMap<>();
    // lista de [atributo, expression]
    private Map<String, List<String[]>> initsCache = new HashMap<>();
    // pre cond
    private Map<String, List<String[]>> preconditionsCache = new HashMap<>();
    // derives
    private Map<String, List<String[]>> derivesCache = new HashMap<>();

    // resultados parseados — populados em loadMode
    private List<Class> classes;
    private List<Association> associations;
    private List<Generalization> generalizations;
    private List<Enumeration> enumerations;
    private List<Interaction> interactions;
    private List<Interface> interfaces;

    public void loadModel(String xmiFile) throws Exception{

        // caminhos dos ficheiros de configuração do SDMetrics (para uml 2.0)
        // carregados dos resources
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

        System.out.println("Model loaded successfully!");

        System.out.println();

        qualifiers = VpParser.parseQualifiers(xmiFile);

        // this was for testing, ocl came from notes in vp xmi
        //loadAnnotationsFromComments();

        // class diagram
        ClassDiagramParser cdp = new ClassDiagramParser(
                model, metaModel, qualifiers,
                invariantsCache, initsCache, preconditionsCache, derivesCache
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

    /*
    * Enquanto nao se faz na ferramenta, o OCL vem em notas
    * notas sao carregadas ao inicializar o parser para poder associar as invariantes às classes certas
     */
    private void loadAnnotationsFromComments(){
        System.out.println("loading ocl annotation");

        List<ModelElement> comments = model.getAcceptedElements(
                metaModel.getType("comment")
        );

        //System.out.println("Found " + comments.size() + " comments.");

        for  (ModelElement comment : comments) {
            String body = comment.getPlainAttribute("body");

            if (body == null){
                continue;
            }

            // tentar parsing de invariante
            Invariant invariant = VpAnnotationsParser.parseInvariant(body);

            // tentar parsing de init se não for invariante
            String[] init = (invariant == null) ? VpAnnotationsParser.parseInit(body) : null;

            // tentar precond
            String[] precondition = (invariant == null && init == null) ? VpAnnotationsParser.parsePreCondition(body) : null;

            // tentar derive
            String[] derive = (invariant == null && init == null && precondition == null) ? VpAnnotationsParser.parseDerive(body) : null;

            if (invariant == null && init == null && precondition == null && derive == null) {
                continue;
            }

            // obter elementos anotados (classe a que a nota está ligada)
            Collection<?> annotatedElements = comment.getSetAttribute("annotatedelement");

            if (annotatedElements == null || annotatedElements.isEmpty()) {
                if (invariant != null) {
                    System.out.println("Invariant '" + invariant.getName() + "' has no annotated elements.");
                } else if (init != null) {
                    System.out.println("Init for '" + init[0] + "' has no annotated elements.");
                } else if (precondition != null) {
                    System.out.println("Precondition '" + precondition[1] + "' has no annotated elements.");
                } else if (derive != null) {
                    System.out.println("Derive for '" + derive[0] + "' has no annotated elements.");
                }
                continue;
            }

            for (Object annotatedElementObj : annotatedElements) {
                ModelElement annotatedElement = (ModelElement) annotatedElementObj;
                String elementId = annotatedElement.getXMIID();

                if (invariant != null) {
                    if (!invariantsCache.containsKey(elementId)) {
                        invariantsCache.put(elementId, new ArrayList<>());
                    }
                    invariantsCache.get(elementId).add(invariant);
                }

                if (init != null) {
                    if (!initsCache.containsKey(elementId)) {
                        initsCache.put(elementId, new ArrayList<>());
                    }
                    initsCache.get(elementId).add(init);
                }

                if (precondition != null) {
                    //System.out.println("precondition found with element id " + elementId + ": " + precondition[1]);
                    if (!preconditionsCache.containsKey(elementId)) {
                        preconditionsCache.put(elementId, new ArrayList<>());
                    }
                    preconditionsCache.get(elementId).add(precondition);
                }

                if (derive != null) {
                    if (!derivesCache.containsKey(elementId)) {
                        derivesCache.put(elementId, new ArrayList<>());
                    }
                    derivesCache.get(elementId).add(derive);
                }
            }

        }
    }

}
