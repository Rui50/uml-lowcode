package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma operação de uma classe UML.
 *
 */
public class Operation {
    private String name;
    private String returnType;
    private String visibility;
    private List<Parameter> parameters;

    public Operation() {
        this.name = "";
        this.returnType = "void";
        this.visibility = "";
        this.parameters = new ArrayList<>();
    }

    public Operation(String name, String returnType, String visibility) {
        this.name = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.parameters = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    @Override
    public String toString() {
        return "Operation {" +
                "name:'" + name + '\'' +
                ", returnType:'" + returnType + '\'' +
                ", visibility:'" + visibility + '\'' +
                ", parameters:" + parameters +
                '}';
    }
}
