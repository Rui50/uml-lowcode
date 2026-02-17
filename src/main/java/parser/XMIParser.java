package parser;
// https://www.sdmetrics.com/javadoc/com/sdmetrics/model/package-summary.html

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sdmetrics.model.MetaModel;
import com.sdmetrics.model.Model;
import com.sdmetrics.model.ModelElement;
import com.sdmetrics.model.XMIReader;
import com.sdmetrics.model.XMITransformations;
import com.sdmetrics.util.XMLParser;

import model.Association;
import model.Attribute;
import model.Class;
import model.Operation;
import model.Parameter;
import model.Relationship;

public class XMIParser {
    private Model model;
    private MetaModel metaModel;

    public void loadModel(String xmiFile) throws Exception{

        // caminhos dos ficheiros de configuração do SDMetrics (para uml 2.0)
        String metaModelURL  = "lib/metamodel2.xml";
        String xmiTransURL = "lib/xmiTrans2_0.xml";

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
    }


    // extrair todos as classes do modelo
    public List<Class> getClasses() {
        List<Class> classes = new ArrayList<>();

        // obtem todos os elementos do tipo class do modelo do SDMetrics
        List<ModelElement> classElements = model.getAcceptedElements(metaModel.getType("class"));

        for (ModelElement element : classElements) {
            Class cl = parseClass(element);
            classes.add(cl);
        }

        return classes;
    }

    private Class parseClass(ModelElement element) {
        String className = element.getName();
        String visibility = element.getPlainAttribute("visibility");
        visibility = visibility != null ? visibility : "public";

        Class cl = new Class(
            className,
            visibility
        );

        Collection<?> attributes = element.getSetAttribute("ownedattributes");
        if (attributes != null) {
            for (Object attributeObj : attributes){
                ModelElement attributeElement = (ModelElement) attributeObj;
                Attribute attribute = parseAttribute(attributeElement);
                cl.addAttribute(attribute);
            }
        }

        Collection<?> operations = element.getSetAttribute("ownedoperations");
        if (operations != null) {
            for (Object operationObj : operations){
                ModelElement operationElement = (ModelElement) operationObj;
                Operation operation = parseOperation(operationElement);
                cl.addOperation(operation);
            }
        }

        return cl;
    }

    private Attribute parseAttribute(ModelElement element) {
        Attribute attribute = new Attribute();

        attribute.setName(element.getName());

        String visibility = element.getPlainAttribute("visibility");
        attribute.setVisibility(visibility != null ? visibility : "public");

        ModelElement type = (ModelElement) element.getRefAttribute("propertytype");
        if (type != null) {
            attribute.setType(type.getName());
        } else {
            attribute.setType("Object");
        }

        return attribute;
    }

    private Operation parseOperation(ModelElement element) {
        Operation operation = new Operation();

        operation.setName(element.getName());

        String visibility = element.getPlainAttribute("visibility");
        operation.setVisibility(visibility != null ? visibility : "public");

       // tipos de retorno e parâmetros
        Collection<?> parameters = element.getSetAttribute("ownedparameters");

        if (parameters != null) {
            for(Object paramObj : parameters) {

                ModelElement paramElement = (ModelElement) paramObj;
                String direction = paramElement.getPlainAttribute("direction");

                if (direction.equals("return")){
                    // é um tipo de retorno
                    ModelElement type = (ModelElement) paramElement.getRefAttribute("propertytype");
                    if (type != null){
                        operation.setReturnType(type.getName());
                    }
                } else {
                    // é um parâmetro de entrada
                    Parameter parameter = parseParameter(paramElement);
                    operation.addParameter(parameter);
                }
            }
        }

        if (operation.getReturnType() == null) {
            operation.setReturnType("void");
        }

        return operation;
    }

    private Parameter parseParameter(ModelElement element) {
        Parameter parameter = new Parameter();

        parameter.setName(element.getName());

        ModelElement type = (ModelElement) element.getRefAttribute("propertytype");
        if (type != null) {
            parameter.setType(type.getName());
        } else {
            parameter.setType("Object");
        }

        return parameter;
    }

    public List<Relationship> getRelationships() {
        List<Relationship> relationships = new ArrayList<>();

        relationships.addAll(parseAssociations());

        return relationships;
    }

    private List<Association> parseAssociations() {
        List<Association> associations =  new ArrayList<>();

        List<ModelElement> associationElements = model.getAcceptedElements(metaModel.getType("association"));

        for (ModelElement associationElement: associationElements){
            // obter os dois lados da associação
            Collection<?> memberEnds = associationElement.getSetAttribute("memberends");

            if (memberEnds != null && memberEnds.size() == 2) {
                // meter numa lista
                List<ModelElement> ends = new ArrayList<>();
                for (Object endObj : memberEnds) {
                    ends.add((ModelElement) endObj);
                }

                // origem
                ModelElement sourceEnd = ends.get(0);
                ModelElement sourceType = (ModelElement) sourceEnd.getRefAttribute("propertytype");

                // destino
                ModelElement targetEnd = ends.get(1);
                ModelElement targetType = (ModelElement) targetEnd.getRefAttribute("propertytype");

                if (sourceType != null && targetType != null) {
                    Association association = new Association();
                    association.setSourceClass(sourceType.getName());
                    association.setTargetClass(targetType.getName());

                    // roles caso tenha
                    association.setSourceRole(sourceEnd.getName());
                    association.setTargetRole(targetEnd.getName());

                    // agregação
                    String aggregation = sourceEnd.getPlainAttribute("aggregation");
                    if (aggregation != null) {
                        association.setAggregationType(aggregation);
                    }

                    // multiplicidade source
                    ModelElement sourceLower = (ModelElement) sourceEnd.getRefAttribute("lowervalue");
                    ModelElement sourceUpper = (ModelElement) sourceEnd.getRefAttribute("uppervalue");

                    String sourceLowerValue = sourceLower != null ? sourceLower.getPlainAttribute("value") : null;
                    String sourceUpperValue = sourceUpper != null ? sourceUpper.getPlainAttribute("value") : null;
                    association.setSourceMultiplicity(buildMultiplicity(sourceLowerValue, sourceUpperValue));

                    // Multiplicidade target
                    ModelElement targetLower = (ModelElement) targetEnd.getRefAttribute("lowervalue");
                    ModelElement targetUpper = (ModelElement) targetEnd.getRefAttribute("uppervalue");
                    String targetLowerValue = targetLower != null ? targetLower.getPlainAttribute("value") : null;
                    String targetUpperValue = targetUpper != null ? targetUpper.getPlainAttribute("value") : null;
                    association.setTargetMultiplicity(buildMultiplicity(targetLowerValue, targetUpperValue));

                    associations.add(association);

                }
            }
        }
        return associations;
    }


    public Model getModel() {
        return model;
    }

    public MetaModel getMetaModel() {
        return metaModel;
    }

    private String buildMultiplicity(String lower, String upper) {
        // ambos null -> sem multiplicidade
        if (lower == null && upper == null) {
            return null;
        }

        // só lower
        if (upper == null) {
            return lower;
        }

        // só upper
        if (lower == null) {
            return upper;
        }

        // ambos iguais -> simplifica "1..1" -> "1"
        if (lower.equals(upper)) {
            return lower;
        }

        // ambos diferentes -> "lower..upper"
        return lower + ".." + upper;
    }

}
