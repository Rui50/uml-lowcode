package model;

import model.annotations.Invariant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// para o vp e o que tem
//https://www.visual-paradigm.com/guide/uml-unified-modeling-language/uml-class-diagram-tutorial/

/**
 * Representa uma classe UML.
 * Identificador JPA (id) — sempre auto-gerado:
 *  - idAttributeName == null  -> id por defeito: @Id @GeneratedValue Long id
 *  - idAttributeName != null  -> @Id @GeneratedValue no atributo escolhido
 *  - idAttributeType          -> tipo Java do id (default Long)
 *  A anotação vive na RAIZ da hierarquia uma classe filha pega no o id com getRoot().
 */
public class Class {

    // propriedades basicas
    private String id;          // xmi:id
    private String name;
    private String visibility;

    // elementos de uma classe
    private List<Attribute> attributes;
    private List<Operation> operations;

    private Class parentClass; // para herança, facilitar geração

    private boolean isAbstract;
    private List<Interface> realizations;

    ///  ANOTACAO

    // estratégia de herança JPA definida na superclass "SINGLE_TABLE" (é o default) | "JOINED"
    private String inheritanceStrategy = "SINGLE_TABLE";

    // nome da tabela JPA (override do utilizador) nao preenchido usa nome da classe em minúsculas)
    private String tableName;

    // gerar o controller REST para esta classe? (Spring)
    private boolean generateController = true;

    // transformar invariantes OCL simples em anotações de Validation em spring
    private boolean beanValidation = true;

    // identificador JPA (sempre auto-gerado)
    // nao indicado entao usa id auto (@Id @GeneratedValue Long id) cpmmo default
    // caso contrário usa esse atributo como @Id @GeneratedValue
    private String idAttributeName;          // null = id autogerado
    private String idAttributeType = "Long"; // tipo Java do id (default Long)

    private List<Invariant> invariants;

    // atributos escondidos em cada DTO (Create/Update/Response).
    private Set<String> hiddenInCreate = new HashSet<>();
    private Set<String> hiddenInUpdate = new HashSet<>();
    private Set<String> hiddenInResponse = new HashSet<>();
    private Map<String, String> collectionOrder = new HashMap<>();   // field da coleção  "attr ASC|DESC" (@OrderBy)

    // override por ENDPOINT (endpointType: List | Get | Create | Update) esse endpoint
    // devolve um ResponseDTO próprio, com campos escondidos independentes do default da classe
    private Map<String, ResponseOverride> responseOverrides = new HashMap<>();


    public Class(String id, String name, String visibility) {
        this.id = id;
        this.name = name;
        this.visibility = visibility;
        this.attributes = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.parentClass = null;
        this.invariants = new ArrayList<>();
        this.realizations = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void addOperation(Operation operation) {
        this.operations.add(operation);
    }

    public Class getParentClass() {
        return parentClass;
    }

    public void setParentClass(Class parentClass) {
        this.parentClass = parentClass;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public String getInheritanceStrategy() {
        return inheritanceStrategy;
    }

    public void setInheritanceStrategy(String inheritanceStrategy) {
        if (inheritanceStrategy != null && !inheritanceStrategy.isBlank()) {
            this.inheritanceStrategy = inheritanceStrategy;
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = (tableName != null && !tableName.isBlank()) ? tableName.trim() : null;
    }

    public boolean isGenerateController() {
        return generateController;
    }

    public void setGenerateController(boolean generateController) {
        this.generateController = generateController;
    }

    public boolean isBeanValidation() {
        return beanValidation;
    }

    public void setBeanValidation(boolean beanValidation) {
        this.beanValidation = beanValidation;
    }

    public String getIdAttributeName() {
        return idAttributeName;
    }

    public void setIdAttributeName(String idAttributeName) {
        this.idAttributeName = (idAttributeName == null || idAttributeName.isBlank()) ? null : idAttributeName;
    }

    public String getIdAttributeType() {
        return idAttributeType;
    }

    public void setIdAttributeType(String idAttributeType) {
        if (idAttributeType == null || idAttributeType.isBlank()) return;
        // int/long -> Integer/Long (o id vai para os generics de JpaRepository<T, ID>)
        this.idAttributeType = fixIdType(idAttributeType);
    }

    // realizations são por classe e não são herdadas (o extends/super.validate já trata a herança)
    public List<Interface> getRealizations() {
        return realizations;
    }

    public void addRealization(Interface i) {
        this.realizations.add(i);
    }

    public List<Invariant> getInvariants() {
        return invariants;
    }

    public void addInvariant(Invariant invariant) {
        this.invariants.add(invariant);
    }

    // Config de DTOs / anotações

    public Set<String> getHiddenInCreate() { return hiddenInCreate; }
    public Set<String> getHiddenInUpdate() { return hiddenInUpdate; }
    public Set<String> getHiddenInResponse() { return hiddenInResponse; }
    public Map<String, String> getCollectionOrder() { return collectionOrder; }

    // escondido se esta classe ou qualquer pai esconder
    public boolean isHiddenInCreate(String attrName) {
        for (Class c = this; c != null; c = c.getParentClass())
            if (c.hiddenInCreate.contains(attrName)) return true;
        return false;
    }
    public boolean isHiddenInUpdate(String attrName) {
        for (Class c = this; c != null; c = c.getParentClass())
            if (c.hiddenInUpdate.contains(attrName)) return true;
        return false;
    }
    public boolean isHiddenInResponse(String attrName) {
        for (Class c = this; c != null; c = c.getParentClass())
            if (c.hiddenInResponse.contains(attrName)) return true;
        return false;
    }

    // endpointType (List|Get|Create|Update) -> responseoverridse
    public Map<String, ResponseOverride> getResponseOverrides() { return responseOverrides; }

    public boolean hasResponseOverride(String endpointType) { return responseOverrides.containsKey(endpointType); }

    // só os overrides desta classe (não herda dos pais).
    public Set<String> responseOverrideHidden(String endpointType) {
        ResponseOverride o = responseOverrides.get(endpointType);
        if (o == null) return Set.of(); // nao ta a esconder nada para este endpoint
        return o.getHidden();
    }

    // nome custom do DTO do override (nao preenchido usa <Class><EndpointType>ResponseDTO)
    public String responseOverrideName(String endpointType) {
        ResponseOverride o = responseOverrides.get(endpointType);
        if (o == null) return "";
        return o.getName();
    }

    // helpers

    public boolean isOrInheritsFrom(String name) {
        Class c = this;
        while (c != null) {
            if (c.getName().equals(name)) return true;
            c = c.getParentClass();
        }
        return false;
    }

    public boolean hasAnyInvariants() {
        return !invariants.isEmpty()
                || realizations.stream().anyMatch(i -> !i.getInvariants().isEmpty())
                || (parentClass != null && parentClass.hasAnyInvariants());
    }

    // raiz da hierarquia (a própria classe se não tiver pai)
    public Class getRoot() {
        Class root = this;
        while (root.getParentClass() != null) {
            root = root.getParentClass();
        }
        return root;
    }

    // a anotação vive na raiz da hierarquia, por isso uma classe filha
    // herda a estratégia do seu pai (raiz).
    public String getRootInheritanceStrategy() {
        return getRoot().getInheritanceStrategy();
    }

    // id "atribuído" pelo cliente (chave natural String) sem @GeneratedValue e incluído no CreateDTO
    // os outros tipos (Long/Integer/UUID) são gerados pelo servidor.
    public boolean isIdAssigned() {
        return "String".equals(getRoot().getIdAttributeType());
    }

    // próprios + herdados
    public List<Attribute> getAllAttributes() {
        List<Attribute> allAttributes = new ArrayList<>(attributes);
        if (parentClass != null) {
            allAttributes.addAll(parentClass.getAllAttributes());
        }
        return allAttributes;
    }

    public List<Operation> getAllOperations() {
        List<Operation> allOperations = new ArrayList<>(operations);
        if (parentClass != null) {
            allOperations.addAll(parentClass.getAllOperations());
        }
        return allOperations;
    }

    private static String fixIdType(String t) {
        switch (t) {
            case "int":   return "Integer";
            case "long":  return "Long";
            case "short": return "Short";
            case "byte":  return "Byte";
            default:      return t;
        }
    }

    @Override
    public String toString() {
        return "Class {" +
                "name:'" + name + '\'' +
                ", visibility:'" + visibility + '\'' +
                ", attributes:" + attributes +
                ", operations:" + operations +
                ", parentClass:'" + (parentClass != null ? parentClass.getName() : null) + '\'' +
                '}';
    }
}
