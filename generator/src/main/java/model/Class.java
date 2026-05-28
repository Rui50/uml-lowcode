package model;

import model.annotations.Invariant;

import java.util.ArrayList;
import java.util.List;

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

    // identificador JPA (sempre auto-gerado)
    // idAttributeName == null -> id sintético (@Id @GeneratedValue Long id) [default]
    // caso contrário          -> usa esse atributo como @Id @GeneratedValue
    private String idAttributeName;          // null = id sintético
    private String idAttributeType = "Long"; // tipo Java do id (default Long)

    private List<Invariant> invariants;

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
