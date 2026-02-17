package model;

import java.util.ArrayList;
import java.util.List;

// para o vp e o que tem
//https://www.visual-paradigm.com/guide/uml-unified-modeling-language/uml-class-diagram-tutorial/

/*
* TODO
* para as associações - Generalizations (Herança), Dependencies, Realizations
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

    // relações
    private List<Association> associations;

    public Class(String name, String visibility) {
        this.name = name;
        this.visibility = visibility;
        this.attributes = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.associations = new ArrayList<>();
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

    public List<Association> getAssociations() {
        return associations;
    }

    public void addAssociation(Association association) {
        this.associations.add(association);
    }

    @Override
    public String toString() {
        return "Class {" +
                "name:'" + name + '\'' +
                ", visibility:'" + visibility + '\'' +
                ", attributes:" + attributes +
                ", operations:" + operations +
                ", relationships:" + associations +
                '}';
    }
}
