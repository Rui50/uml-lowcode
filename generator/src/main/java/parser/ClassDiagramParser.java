package parser;

import com.sdmetrics.model.MetaModel;
import com.sdmetrics.model.Model;
import com.sdmetrics.model.ModelElement;
import generator.GeneratorUtils;
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

    private Map<String, Qualifier> qualifiers;

    public ClassDiagramParser(
            Model model,
            MetaModel metaModel,
            Map<String, Qualifier> qualifiers
    ){
        this.model = model;
        this.metaModel = metaModel;
        this.qualifiers = qualifiers;
    }

    // a func getSetAttribute(...) devolve ordens diferentes de cada vez
    // por isso ordenar pelo comparador do SDMetrics para tornar a geração determinística
    private static List<ModelElement> ordered(Collection<?> set) {
        List<ModelElement> list = new ArrayList<>();
        if (set != null) for (Object o : set) list.add((ModelElement) o);
        list.sort(ModelElement.getComparator());
        return list;
    }

    /**
     * extrai as classes do modelo UML
     */
    public List<Class> parseClasses() {
        List<Class> classes = new ArrayList<>();

        // obtem todos os elementos do tipo class do modelo do SDMetrics
        List<ModelElement> classElements = model.getAcceptedElements(metaModel.getType("class"));

        for (ModelElement element : classElements) {
            Class cl = parseClass(element);
            classes.add(cl);
        }
        return classes;
    }

    /**
     * fazer o parsing de todas as infos de uma classe
     */
    private Class parseClass(ModelElement element) {
        // nome e visibilidade da classe, assumir public se não tiver
        String className = element.getName();
        String visibility = element.getPlainAttribute("visibility");
        visibility = visibility != null ? visibility : "public";

        // criar a classe
        String classId = element.getXMIID();
        Class cl = new Class(classId, className, visibility);

        // determinar se é uma classe abstrata
        String abstractAttr = element.getPlainAttribute("abstract");
        cl.setAbstract("true".equalsIgnoreCase(abstractAttr));

        // todos os atributos da classe
        for (ModelElement attributeElement : ordered(element.getSetAttribute("ownedattributes"))) {
            // as associações também aparecem como atributos
            // tirar porque são tratados depois independentemente
            ModelElement associationRef = (ModelElement) attributeElement.getRefAttribute("association");
            if (associationRef != null) {
                continue;
            }

            // parsing dos atributos "normais"
            Attribute attribute = parseAttribute(attributeElement);
            cl.addAttribute(attribute);
        }

        // todas as operações da classe
        for (ModelElement operationElement : ordered(element.getSetAttribute("ownedoperations"))) {
            Operation operation = parseOperation(operationElement);
            cl.addOperation(operation);
        }

        return cl;
    }

    /**
     * extrai todas as interfaces do  UML.
     * interface tem nome, visibilidade e operations.
     */
    public List<Interface> parseInterfaces() {
        List<Interface> interfaces = new ArrayList<>();

        // obtem todos os elementos do tipo interface
        List<ModelElement> interfaceElements = model.getAcceptedElements(metaModel.getType("interface"));

        // cada uma nome, public, id, operations
        for (ModelElement element : interfaceElements) {
            String name = element.getName();
            Interface iface = new Interface(element.getXMIID(), name, "public");

            // operations dessa interface
            for (ModelElement operationElement : ordered(element.getSetAttribute("ownedoperations"))) {
                Operation operation = parseOperation(operationElement);
                iface.addOperation(operation);
            }
            interfaces.add(iface);
        }
        return interfaces;
    }

    // para cada atributo obter nome, tipo e visibilidade
    private Attribute parseAttribute(ModelElement element) {
        Attribute attribute = new Attribute();

        attribute.setName(element.getName());

        String visibility = element.getPlainAttribute("visibility");
        attribute.setVisibility(visibility != null ? visibility : "public");

        // nas coleções o o trigger gcattrval apanha o xmi:value de todos os netos de
        // <xmi:Extension>, logo apanha o <genericType> e outras coisas tambem (isAbstract/isVisble)
        // Só o tipo da coleção tem '<'
        String generic = null;
        for (Object o : element.getSetAttribute("generictype")) {
            String v = String.valueOf(o);
            if (v.contains("<")) {
                generic = v;
                break;
            }
        }
        if (generic != null) {
            String kind = generic.substring(0, generic.indexOf('<')).trim(); // List / Set
            String elem = generic.substring(generic.indexOf('<') + 1, generic.lastIndexOf('>')).trim();
            // so suporta List/Set e tipos basicos
            if ((kind.equals("List") || kind.equals("Set")) && GeneratorUtils.isSimpleParamType(elem)) {
                attribute.setType(GeneratorUtils.fixType(GeneratorUtils.removePackageNames(elem))); // tipo do elemento,
                attribute.setCollectionType(kind);  // "List" / "Set"
                return attribute;
            }
            throw new RuntimeException("Atributo '" + element.getName() + "' tem tipo generico '" + generic
                    + "' nao suportado (só List/Set de tipos basicos).");
        }

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

        // visibilidade da operação
        String visibility = element.getPlainAttribute("visibility");
        operation.setVisibility(visibility != null ? visibility : "public");

        // se é op abstrata
        String abstractAttr = element.getPlainAttribute("abstract");
        operation.setAbstract("true".equalsIgnoreCase(abstractAttr));

        // tipos de return e parâmetros
        for (ModelElement paramElement : ordered(element.getSetAttribute("ownedparameters"))) {
            // direction do parâmetro (in, out, return)
            String direction = paramElement.getPlainAttribute("kind");

            if ("return".equals(direction)){
                // é um tipo de return

                // genericType é como o VP da handle a Lists etc
                String genericType = paramElement.getPlainAttribute("generictype");
                if (genericType != null && !genericType.trim().isEmpty()) {
                    //  (List<model.Aluno>)
                    operation.setReturnType(GeneratorUtils.removePackageNames(genericType));
                }
                // se não tiver genericType, tenta o tipo normal
                else {
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

        // genericType = lists etc
        String genericType = element.getPlainAttribute("generictype");

        if (genericType != null && !genericType.trim().isEmpty()) {
            //  (List<model.Aluno>)
            parameter.setType(GeneratorUtils.removePackageNames(genericType));
        } else {
            // tipos "normais"
            ModelElement type = (ModelElement) element.getRefAttribute("parametertype");
            if (type != null) {
                parameter.setType(type.getName());
            } else {
                parameter.setType("Object");
            }
        }

        return parameter;
    }

    /**
     * extrai as associações do modelo, incluindo multiplicidade, roles, navegabilidade e agregação.
     * assume sempre que tem exatamente 2 "ends"
     * assume tambem que ends.get(0) é source e ends.get(1) é target
     */
    public List<Association> parseAssociations() {
        List<Association> associations =  new ArrayList<>();

        // todos os elementos de associação
        List<ModelElement> associationElements = model.getAcceptedElements(metaModel.getType("association"));

        for (ModelElement associationElement: associationElements){
            // obter os dois lados da associação
            Collection<?> memberEnds = associationElement.getSetAttribute("memberends");

            // so pode ter associações com 2 lados
            if (memberEnds != null && memberEnds.size() == 2) {
                // meter os dois lados numa lista, considerar a origim como o primeiro e o destino como o segundo se for necessário
                List<ModelElement> ends = ordered(memberEnds);

                // origem assume sempre como sendo o get(0) da assoc
                ModelElement sourceEnd = ends.get(0);
                ModelElement sourceType = (ModelElement) sourceEnd.getRefAttribute("propertytype");

                // destino
                ModelElement targetEnd = ends.get(1);
                ModelElement targetType = (ModelElement) targetEnd.getRefAttribute("propertytype");

                // obter qualifiers de cada lado da associação, se existirem
                Qualifier sourceQualifier = qualifiers.get(sourceEnd.getXMIID());
                Qualifier targetQualifier = qualifiers.get(targetEnd.getXMIID());

                if (sourceType != null && targetType != null) {
                    Association association = new Association();
                    association.setSourceClass(sourceType.getName());
                    association.setTargetClass(targetType.getName());

                    // roles caso tenha
                    association.setSourceRole(sourceEnd.getName());
                    association.setTargetRole(targetEnd.getName());

                    String sourceAgg = sourceEnd.getPlainAttribute("aggregation");
                    String targetAgg = targetEnd.getPlainAttribute("aggregation");
                    String aggregation;
                    // none  = no type of agg
                    if (!"none".equals(sourceAgg)) {
                        aggregation = sourceAgg;
                    } else {
                        aggregation = targetAgg;
                    }
                    if (aggregation != null) {
                        association.setAggregationType(aggregation);
                    }

                    if ("composite".equals(aggregation)) {
                        association.setCompositeOnSource("composite".equals(sourceAgg));
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

                    // só se suporta 1 qualifier por associação (o Map fica numa ponta só)
                    if (sourceQualifier != null && targetQualifier != null) {
                        throw new IllegalArgumentException("Associação " + association.getSourceClass()
                                + " <-> " + association.getTargetClass() + " tem qualificador nas DUAS pontas — "
                                + "não suportado (um qualificador/Map por associação).");
                    }
                    if(sourceQualifier != null) {
                        association.setQualifierName(sourceQualifier.name);
                        association.setQualifierType(sourceQualifier.type);
                        association.setQualifierOnSource(true);
                        //System.out.println("Association " + association.getSourceClass() + " -> " + association.getTargetClass() + " has qualifier: " + sourceQualifier + " (onSource=true)");
                    } else if (targetQualifier != null) {
                        association.setQualifierName(targetQualifier.name);
                        association.setQualifierType(targetQualifier.type);
                        association.setQualifierOnSource(false);
                        //System.out.println("Association " + association.getSourceClass() + " -> " + association.getTargetClass() + " has qualifier: " + targetQualifier + " (onSource=false)");
                    }

                    associations.add(association);

                }
            }
        }
        return associations;
    }

    // extrair enumerações do modelo
    public List<Enumeration> parseEnumerations() {
        List<Enumeration> enumerations = new ArrayList<>();

        // pegas nas enumerations do modelo
        List<ModelElement> enumElements = model.getAcceptedElements(metaModel.getType("enumeration"));

        for (ModelElement enumElement : enumElements) {
            Enumeration enumeration = new Enumeration(enumElement.getName());

            // adiciona os enums
            for (ModelElement literalElement : ordered(enumElement.getSetAttribute("ownedliterals"))) {
                enumeration.addLiteral(literalElement.getName());
            }

            enumerations.add(enumeration);
        }

        return enumerations;
    }

    /**
     * herança entre classes cada generalization tem um parent e um child.
     */
    public List<Generalization> parseGeneralizations() {
        List<Generalization> generalizations = new ArrayList<>();

        // obtem as associações de herança do modelo
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

                //System.out.println("Generalization found: " + childClassName + " -> " + parentClassName);
            }
            else {
                System.out.println("Generalization element with missing references: " + generalizationElement.getName());
            }
        }

        return generalizations;
    }

    /**
     * Liga cada classe às interfaces que realiza.
     * VP exporta como <packagedElement xmi:type="uml:Realization">, com
     *  client  = a classe que implementa
     *  supplier = a interface realizada
     */
    public void fillRealizations(List<Class> classes, List<Interface> interfaces) {
        List<ModelElement> realizationElements = model.getAcceptedElements(metaModel.getType("realization"));


        for (ModelElement rel : realizationElements) {
            Collection<?> clients = rel.getSetAttribute("client");
            Collection<?> suppliers = rel.getSetAttribute("supplier");
            if (clients == null || suppliers == null) continue;

            for (Object client : clients) {
                ModelElement clientElem = (ModelElement) client;
                Class cl = GeneratorUtils.findClassByName(classes, clientElem.getName());
                if (cl == null) continue;

                for (Object sObj : suppliers) {
                    ModelElement supplierElem = (ModelElement) sObj;
                    // só conta se o supplier for uma interface
                    for (Interface i : interfaces) {
                        if (i.getName().equals(supplierElem.getName())) {
                            cl.addRealization(i);
                            break;
                        }
                    }
                }
            }
        }

        mergeInterfaceOperations(classes);
    }

    // preenche a herança entre classes (parent link) a partir das generalizations já parseadas.
    public void fillGeneralizations(List<Class> classes, List<Generalization> generalizations) {
        for (Generalization gen : generalizations) {
            Class child = GeneratorUtils.findClassByName(classes, gen.getChildClassName());
            Class parent = GeneratorUtils.findClassByName(classes, gen.getParentClassName());
            if (child != null && parent != null) {
                child.setParentClass(parent);
            }
        }
    }

    // garante que a classe tem as ops das interfaces que realiza, evitar que n compile
    // verifica por nome as que falta
    private void mergeInterfaceOperations(List<Class> classes) {
        for (Class cls : classes) {
            for (Interface iface : cls.getRealizations()) {
                for (Operation op : iface.getOperations()) {
                    // se a classe já tiver uma op com o mesmo nome, não clona
                    boolean alreadyHas = cls.getAllOperations().stream()
                            .anyMatch(o -> o.getName().equals(op.getName()));
                    if (!alreadyHas) cls.addOperation(cloneOperation(op, cls));
                }
            }
        }
    }

    // clone por classe (id ifaceOpId@Classe) para cada classe ter a sua op, com id próprio porque as anotações
    // da frontend usam chaves por id
    private static Operation cloneOperation(Operation src, Class cls) {
        Operation copy = new Operation(src.getId() + "@" + cls.getName(),
                src.getName(), src.getReturnType(), src.getVisibility());
        for (Parameter p : src.getParameters()) {
            copy.addParameter(new Parameter(p.getName(), p.getType(), p.getDirection()));
        }
        return copy;
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
