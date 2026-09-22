package model;

import java.util.ArrayList;
import java.util.List;
import model.sequence.Interaction;

// modelo inteiro para mandar para a frontend etc
public class ParsedModel {
    private List<Class> classes;
    private List<Association> associations;
    private List<Generalization> generalizations;
    private List<Enumeration> enumerations;
    private List<Interaction> psmInteractions;
    private List<Interaction> pimInteractions;
    private List<Interaction> allInteractions;
    private List<Interface> interfaces;
    private final List<String> warnings = new ArrayList<>();   // avisos do parse

    public ParsedModel(List<Class> classes,
                       List<Association> associations,
                       List<Generalization> generalizations,
                       List<Enumeration> enumerations,
                       List<Interaction> psmInteractions,
                       List<Interaction> pimInteractions,
                       List<Interaction> allInteractions,
                       List<Interface> interfaces
    )
    {
        this.classes = classes;
        this.associations = associations;
        this.generalizations = generalizations;
        this.enumerations = enumerations;
        this.psmInteractions = psmInteractions;
        this.pimInteractions = pimInteractions;
        this.allInteractions = allInteractions;
        this.interfaces = interfaces;
    }

    public List<Class> getClasses() {
        return classes;
    }

    public void setClasses(List<Class> classes) {
        this.classes = classes;
    }

    public List<Association> getAssociations() {
        return associations;
    }

    public void setAssociations(List<Association> associations) {
        this.associations = associations;
    }

    public List<Generalization> getGeneralizations() {
        return generalizations;
    }

    public void setGeneralizations(List<Generalization> generalizations) {
        this.generalizations = generalizations;
    }

    public List<Enumeration> getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(List<Enumeration> enumerations) {
        this.enumerations = enumerations;
    }

    public List<Interaction> getPsmInteractions() {
        return psmInteractions;
    }

    public void setPsmInteractions(List<Interaction> psmInteractions) {
        this.psmInteractions = psmInteractions;
    }

    public List<Interaction> getPimInteractions() {
        return pimInteractions;
    }

    public void setPimInteractions(List<Interaction> pimInteractions) {
        this.pimInteractions = pimInteractions;
    }

    public List<Interaction> getAllInteractions() {
        return allInteractions;
    }

    public List<Interface> getInterfaces() { return interfaces; }

    public List<String> getWarnings() { return warnings; }

}

