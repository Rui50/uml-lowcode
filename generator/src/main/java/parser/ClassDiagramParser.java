package parser;

import com.sdmetrics.model.MetaModel;
import com.sdmetrics.model.Model;
import com.sdmetrics.model.ModelElement;
import model.*;
import model.Class;
import model.Enumeration;
import model.annotations.Invariant;
import model.annotations.Precondition;
import ocl.OCLToJavaVisitor;
import parser.VpParser.Qualifier;

import java.util.*;

public class ClassDiagramParser {
    private Model model;
    private MetaModel metaModel;

    // estes sao temps porque depois passa a ser na "app" e nao no xmi parsing
    private Map<String, Qualifier> qualifiers;
    private Map<String, List<Invariant>> invariantsCache;
    private Map<String, List<String[]>> initsCache;
    private Map<String, List<String[]>> preconditionsCache;
    private Map<String, List<String[]>> derivesCache;

    public ClassDiagramParser(
            Model model,
            MetaModel metaModel,
            Map<String, Qualifier> qualifiers,
            Map<String, List<Invariant>> invariantsCache,
            Map<String, List<String[]>> initsCache,
            Map<String, List<String[]>> preconditionsCache,
            Map<String, List<String[]>> derivesCache
    ){
        this.model = model;
        this.metaModel = metaModel;
        this.qualifiers = qualifiers;
        this.invariantsCache = invariantsCache;
        this.initsCache = initsCache;
        this.preconditionsCache = preconditionsCache;
        this.derivesCache = derivesCache;
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
        // nome e visibilidade da classe, assumir public
        String className = element.getName();
        String visibility = element.getPlainAttribute("visibility");
        visibility = visibility != null ? visibility : "public";

        Class cl = new Class(
                className,
                visibility
        );

        // attr da classe
        Collection<?> attributes = element.getSetAttribute("ownedattributes");
        if (attributes != null) {
            for (Object attributeObj : attributes){
                ModelElement attributeElement = (ModelElement) attributeObj;

                // as associações também aparecem como atributos
                // tirar porque são tratados depois
                ModelElement associationRef = (ModelElement) attributeElement.getRefAttribute("association");
                if (associationRef != null) {
                    continue;
                }

                // parsing dos atributos "normais"
                Attribute attribute = parseAttribute(attributeElement);
                cl.addAttribute(attribute);
            }
        }

        // operações da classe
        Collection<?> operations = element.getSetAttribute("ownedoperations");
        if (operations != null) {
            for (Object operationObj : operations){
                ModelElement operationElement = (ModelElement) operationObj;
                Operation operation = parseOperation(operationElement);
                cl.addOperation(operation);
            }
        }

        String classId = element.getXMIID();
        List<Invariant> invariants = invariantsCache.get(classId);

        if (invariants != null) {
            for (Invariant invariant : invariants) {
                cl.addInvariant(invariant);
            }}

        // aplicar inits aos atributos
        List<String[]> inits = initsCache.get(classId);
        if (inits != null) {
            for (String[] init : inits) {
                String attrName = init[0];
                String expression = init[1];

                // encontrar o atributo correspondente
                for (Attribute attr : cl.getAttributes()) {
                    if (attr.getName().equals(attrName)) {
                        attr.setInitExpression(expression);
                        attr.setJavaInitExpression(OCLToJavaVisitor.toJava(expression));
                        break;
                    }
                }
            }
        }

        // aplicar precondições as operações
        List<String[]> preconditions = preconditionsCache.get(classId);
        if (preconditions != null) {
            for (String[] pre : preconditions) {
                String operationName = pre[0];
                String preName = pre[1];
                String expression = pre[2];

                // encontrar a operação correspondente
                for (Operation op : cl.getOperations()) {
                    if (op.getName().equals(operationName)) {
                        Precondition precondition = new Precondition(preName, expression, "Precondition '" + preName + "' violated.");
                        precondition.setJavaExpression(OCLToJavaVisitor.toJava(expression));
                        op.addPrecondition(precondition);
                        break;
                    }
                }
            }
        }

        // derive part
        List<String[]> derives = derivesCache.get(classId);
        if (derives != null) {
            for (String[] derive : derives) {
                String attrName = derive[0];
                String expression = derive[1];
                for (Attribute attr : cl.getAttributes()) {
                    if (attr.getName().equals(attrName)) {
                        attr.setDeriveExpression(expression);
                        attr.setJavaDeriveExpression(OCLToJavaVisitor.toJava(expression));
                        break;
                    }
                }
            }
        }

        return cl;
    }

    // para cada atributo obter nome, tipo e visibilidade
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

    /**
     * para cada operação obter nome visibilidade
     * tipos no return e parâmetros
     */
    private Operation parseOperation(ModelElement element) {
        Operation operation = new Operation();

        operation.setId(element.getXMIID());
        operation.setName(element.getName());

        String visibility = element.getPlainAttribute("visibility");
        operation.setVisibility(visibility != null ? visibility : "public");

        // tipos de return e parâmetros
        Collection<?> parameters = element.getSetAttribute("ownedparameters");

        if (parameters != null) {
            for(Object paramObj : parameters) {

                ModelElement paramElement = (ModelElement) paramObj;
                // direction do parâmetro (in, out, return)
                String direction = paramElement.getPlainAttribute("kind");

                if (direction.equals("return")){
                    // é um tipo de return

                    // genericType é como o VP da handle a Lists etc
                    String genericType = paramElement.getPlainAttribute("generictype");
                    if (genericType != null && !genericType.trim().isEmpty()) {
                        operation.setReturnType(genericType);
                    } else {
                        ModelElement type = (ModelElement) paramElement.getRefAttribute("parametertype");
                        if (type != null) {
                            operation.setReturnType(type.getName());
                        }
                    }
                } else {
                    // Parâmetro normal
                    Parameter parameter = parseParameter(paramElement, direction);
                    operation.addParameter(parameter);
                }
            }
        }

        if (operation.getReturnType() == null) {
            operation.setReturnType("void");
        }

        return operation;
    }


    /****
     * para cada parâmetro obter nome, tipo e direção (in, out, inout)
     */
    private Parameter parseParameter(ModelElement element, String direction) {
        Parameter parameter = new Parameter();

        parameter.setName(element.getName());

        // direction
        parameter.setDirection(direction != null ? direction : "in");

        String genericType = element.getPlainAttribute("generictype");

        if (genericType != null && !genericType.trim().isEmpty()) {
            parameter.setType(genericType);
        } else {
            // fallback
            ModelElement type = (ModelElement) element.getRefAttribute("parametertype");
            if (type != null) {
                parameter.setType(type.getName());
            } else {
                parameter.setType("Object");
            }
        }

        return parameter;
    }

    public List<Association> parseAssociations() {
        List<Association> associations =  new ArrayList<>();

        List<ModelElement> associationElements = model.getAcceptedElements(metaModel.getType("association"));

        for (ModelElement associationElement: associationElements){
            // obter os dois lados da associação
            Collection<?> memberEnds = associationElement.getSetAttribute("memberends");

            if (memberEnds != null && memberEnds.size() == 2) {
                // meter os dois lados numa lista, considerar a origim como o primeiro e o destino como o segundo se for necessário
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

                // obter qualifiers de cada lado da associação, se existirem
                VpParser.Qualifier sourceQualifier = qualifiers.get(sourceEnd.getXMIID());
                VpParser.Qualifier targetQualifier = qualifiers.get(targetEnd.getXMIID());

                // para obter os qualifiers do parsing Manual do vp
                String sourceEndId = sourceEnd.getXMIID();
                String targetEndId = targetEnd.getXMIID();

                if (sourceType != null && targetType != null) {
                    Association association = new Association();
                    association.setSourceClass(sourceType.getName());
                    association.setTargetClass(targetType.getName());

                    // roles caso tenha
                    association.setSourceRole(sourceEnd.getName());
                    association.setTargetRole(targetEnd.getName());

                    // agregação (só pode estar num dos lados)
                    String aggregation = sourceEnd.getPlainAttribute("aggregation");
                    if (aggregation == null) {
                        aggregation = targetEnd.getPlainAttribute("aggregation");
                    }
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

                    // navegability
                    String sourceNavigable = sourceEnd.getPlainAttribute("isnavigable");
                    String targetNavigable = targetEnd.getPlainAttribute("isnavigable");
                    association.setSourceNavigable(sourceNavigable == null || Boolean.parseBoolean(sourceNavigable));
                    association.setTargetNavigable(targetNavigable == null || Boolean.parseBoolean(targetNavigable));
                    

                    if(sourceQualifier != null) {
                        association.setQualifierName(sourceQualifier.name);
                        association.setQualifierType(sourceQualifier.type);
                        System.out.println("Association " + association.getSourceClass() + " -> " + association.getTargetClass() + " has qualifier: " + sourceQualifier);
                    } else if (targetQualifier != null) {
                        association.setQualifierName(targetQualifier.name);
                        association.setQualifierType(targetQualifier.type);
                        System.out.println("Association " + association.getSourceClass() + " -> " + association.getTargetClass() + " has qualifier: " + targetQualifier);
                    }

                    associations.add(association);

                }
            }
        }
        return associations;
    }

    // extrair enumerações do modelo
    public List<Enumeration> getEnumerations() {
        List<Enumeration> enumerations = new ArrayList<>();

        List<ModelElement> enumElements = model.getAcceptedElements(metaModel.getType("enumeration"));

        for (ModelElement enumElement : enumElements) {
            Enumeration enumeration = new Enumeration(enumElement.getName());

            Collection<?> literals = enumElement.getSetAttribute("ownedliterals");
            if (literals != null) {
                for (Object literalObj : literals) {
                    ModelElement literalElement = (ModelElement) literalObj;
                    enumeration.addLiteral(literalElement.getName());
                }
            }

            enumerations.add(enumeration);
        }

        return enumerations;
    }

    /**
     * Funções relacionadas a herança
     */
    public List<Generalization> parseGeneralizations() {
        List<Generalization> generalizations = new ArrayList<>();

        List<ModelElement> generalizationElements = model.getAcceptedElements(metaModel.getType("generalization"));


        for (ModelElement generalizationElement : generalizationElements) {
            // classe que herda
            ModelElement subclassElement = (ModelElement) generalizationElement.getRefAttribute("context");

            // classe pai
            ModelElement parentclassElement = (ModelElement) generalizationElement.getRefAttribute("general");

            if (subclassElement != null && parentclassElement != null) {
                String childClassName = subclassElement.getName();
                String parentClassName = parentclassElement.getName();

                Generalization generalization = new Generalization(childClassName, parentClassName);
                generalizations.add(generalization);

                System.out.println("Generalization found: " + childClassName + " -> " + parentClassName);
            }
            else {
                System.out.println("Generalization element with missing references: " + generalizationElement.getName());
            }
        }

        return generalizations;
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
