package model.sequence;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String id;
    private String name;                    // getTurma, setTurma (just the method name)
    private String rawName;                 // full original: "getTurma(a: Aluno) : Turma"
    private String messageSort;             // synchCall, asynchCall, reply
    private String returnType;              // return type if specified (e.g., "User")
    private String assignVariable;          // variable name for assignment (e.g., "turma" in "turma = findById(id) : Turma")
    private Lifeline source;
    private Lifeline target;
    private List<MessageArgument> arguments;
    private int sequenceNumber;             // Order
    private String vpNumber;


    public Message(){
        this.arguments = new ArrayList<>();
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

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getAssignVariable() {
        return assignVariable;
    }

    public void setAssignVariable(String assignVariable) {
        this.assignVariable = assignVariable;
    }

    public String getMessageSort() {
        return messageSort;
    }

    public void setMessageSort(String messageSort) {
        this.messageSort = messageSort;
    }

    public Lifeline getSource() {
        return source;
    }

    public void setSource(Lifeline source) {
        this.source = source;
    }

    public Lifeline getTarget() {
        return target;
    }

    public void setTarget(Lifeline target) {
        this.target = target;
    }

    public List<MessageArgument> getArguments() {
        return arguments;
    }

    public void setArguments(List<MessageArgument> arguments) {
        this.arguments = arguments;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getVpNumber() {
        return vpNumber;
    }

    public void setVpNumber(String vpNumber) {
        this.vpNumber = vpNumber;
    }

    @Override
    public String toString() {
        return "Message {" +
                "name:'" + name + '\'' +
                (assignVariable != null ? ", assignVariable:'" + assignVariable + "'" : "") +
                ", messageSort:'" + messageSort + '\'' +
                ", source:" + (source != null ? source.getType() : "null") +
                ", target:" + (target != null ? target.getType() : "null") +
                ", vpNumber:'" + vpNumber + '\'' +
                ", arguments:" + arguments +
                (returnType != null ? ", returnType:'" + returnType + "'" : "") +
                '}';
    }
}
