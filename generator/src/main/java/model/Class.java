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
