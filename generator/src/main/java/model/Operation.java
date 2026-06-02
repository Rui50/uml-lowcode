package model;

import java.util.ArrayList;
import java.util.List;

import model.annotations.Postcondition;
import model.annotations.Precondition;
import model.sequence.Interaction;

/**
 * Representa uma operação de uma classe UML.
 *
 */
public class Operation {
    private String id;
    private String name;
    private String returnType;
    private String visibility;
    private boolean isAbstract;
    private List<Parameter> parameters;

    private Interaction sequenceDiagram;

    private List<Precondition> preconditions = new ArrayList<>();

    private List<Postcondition> postconditions = new ArrayList<>();

    // operação é um job @Scheduled (Spring) sem endpoint REST, sem parâmetros/caller
    private boolean isJob = false;
    private String cron;                    // expressão cron do job (só usado quando isJob)

    // endpoint REST custom (operações que nao sao job)
    private String endpointName;            // path; null/vazio = usa o nome da operação
    private String httpMethod = "POST";     // HTTP: GET | POST | PUT | DELETE | PATCH


    public Operation() {
        this.id = "";
        this.name = "";
        this.returnType = "void";
        this.visibility = "";
        this.parameters = new ArrayList<>();
    }

    public Operation(String id, String name, String returnType, String visibility) {
        this.id = id;
        this.name = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.parameters = new ArrayList<>();
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

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public List<Precondition> getPreconditions() {
        return preconditions;
    }

    public void addPrecondition(Precondition precondition) {
        this.preconditions.add(precondition);
    }

    public Interaction getSequenceDiagram() {
        return sequenceDiagram;
    }

    public void setSequenceDiagram(Interaction sequenceDiagram) {
        this.sequenceDiagram = sequenceDiagram;
    }

    public List<Postcondition> getPostconditions() {
        return postconditions;
    }

    public void addPostcondition(Postcondition postcondition) {
        this.postconditions.add(postcondition);
    }

    public boolean isJob() {
        return isJob;
    }

    public void setJob(boolean isJob) {
        this.isJob = isJob;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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
