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

/*
* TODO
* para as associações - Dependencies, Realizations
*
* */


/**
 * Representa uma classe UML.
 *
 * Identificador JPA (id) — sempre auto-gerado:
 *  - idAttributeName == null  -> id por defeito: @Id @GeneratedValue Long id
 *  - idAttributeName != null  -> @Id @GeneratedValue no atributo escolhido
 *  - idAttributeType          -> tipo Java do id (default Long)
 *  A anotação vive na RAIZ da hierarquia; uma classe filha resolve o id via getRoot().
 */
public class Class {
    // propriedades basicas para já
    private String name;
    private String visibility;

    // elementos de uma classe
    private List<Attribute> attributes;
    private List<Operation> operations;

    private Class parentClass; // para herança, facilitar geração

    private boolean isAbstract;
    private List<Interface> realizations;

    // estratégia de herança JPA, definida na superclasse
    // "SINGLE_TABLE" (default) | "JOINED"
    private String inheritanceStrategy = "SINGLE_TABLE";

    // nome da tabela JPA (override do utilizador); null/vazio = derivado (nome da classe em minúsculas)
    private String tableName;

    // gerar o controller REST para esta classe? (Spring) — default true
    private boolean generateController = true;

    // transformar invariantes OCL simples em anotações de Validation em spring — default true
    private boolean beanValidation = true;

    // identificador JPA (sempre auto-gerado)
    // idAttributeName == null -> id sintético (@Id @GeneratedValue Long id) [default]
    // caso contrário          -> usa esse atributo como @Id @GeneratedValue
    private String idAttributeName;          // null = id sintético
    private String idAttributeType = "Long"; // tipo Java do id (default Long)

    private List<Invariant> invariants;

    // Atributos escondidos de cada DTO (Create/Update/Response) desta classe.
    // HERDA-SE: esconder é sobre o ATRIBUTO, e o atributo é herdado -> isHiddenIn* une pela cadeia
    // de pais (se o pai esconde 'email', a subclasse também, senão reexpunha-o).
    // Na CLASSE e não no Attribute: instâncias de atributos herdados são partilhadas com o pai,
    // logo uma flag lá vazaria para os irmãos.
    private Set<String> hiddenInCreate = new HashSet<>();
    private Set<String> hiddenInUpdate = new HashSet<>();
    private Set<String> hiddenInResponse = new HashSet<>();

    public Set<String> getHiddenInCreate() { return hiddenInCreate; }
    public Set<String> getHiddenInUpdate() { return hiddenInUpdate; }
    public Set<String> getHiddenInResponse() { return hiddenInResponse; }

    // Override do response POR ENDPOINT (endpointType: List | Get | Create | Update): esse endpoint
    // devolve um ResponseDTO próprio, com campos escondidos independentes do default da classe.
    // NÃO se herda (ao contrário dos hiddenIn*): é sobre um ENDPOINT, não um atributo. Cada classe
    // tem o seu controller/URLs (List do Aluno != List do AlunoTE) -> configura-se por classe.
    private Map<String, ResponseOverride> responseOverrides = new HashMap<>();

    // mapa keyed pelo endpointType (List|Get|Create|Update); itera-se por entrySet/keySet
    public Map<String, ResponseOverride> getResponseOverrides() { return responseOverrides; }

    public boolean hasResponseOverride(String endpointType) { return responseOverrides.containsKey(endpointType); }

    // não herda (endpoint-level, ver responseOverrides); só o mapa desta classe
    public Set<String> responseOverrideHidden(String endpointType) {
        ResponseOverride o = responseOverrides.get(endpointType);
        if (o == null) return Set.of();   // sem override -> nada escondido
        return o.getHidden();
    }

    // nome custom do DTO do override (vazio = usar o auto <Class><EndpointType>ResponseDTO)
    public String responseOverrideName(String endpointType) {
        ResponseOverride o = responseOverrides.get(endpointType);
        if (o == null) return "";
        return o.getName();
    }

    // escondido se ESTA classe ou qualquer antepassado o esconder (união pela cadeia de pais)
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

    public Class(String name, String visibility) {
        this.name = name;
        this.visibility = visibility;
        this.attributes = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.parentClass = null;
        this.invariants = new ArrayList<>();
        this.realizations = new ArrayList<>();
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
        // só guarda um override real; vazio/branco mantém o nome derivado
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
        // ids têm de ser tipos de referência (generics em JpaRepository<>, List<>, etc.
        // não aceita primitivos
        this.idAttributeType = fixIdType(idAttributeType);
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

    // id "atribuído" pelo cliente (chave natural String): sem @GeneratedValue e
    // incluído no CreateDTO. Os restantes (Long/Integer/UUID) são gerados pelo servidor.
    public boolean isIdAssigned() {
        return "String".equals(getRoot().getIdAttributeType());
    }

    //s realizations são por classe e não são herdadas.
    // Se uma interface for realizada por uma classe abstrata, as subclasses
    // concretas não a herdam (getRealizations() devolve só as próprias).
    // Para suportar, é preciso percorrer a cadeia de pais, como em isOrInheritsFrom/super.validate().
    public List<Interface> getRealizations() {
        return realizations;
    }

    public void addRealization(Interface i) {
        this.realizations.add(i);
    }

    // caso de ser uma classe filha, trazer os atributos e operações da classe pai
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

    public List<Invariant> getInvariants() {
        return invariants;
    }

    public void addInvariant(Invariant invariant) {
        this.invariants.add(invariant);
    }

    @Override
    public String toString() {
        return "Class {" +
                "name:'" + name + '\'' +
                ", visibility:'" + visibility + '\'' +
                ", attributes:" + attributes +
                ", operations:" + operations +
                ", parentClass:'" + parentClass + '\'' +
                '}';
    }
}
