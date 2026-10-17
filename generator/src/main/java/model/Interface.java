package model;

import model.annotations.Invariant;

import java.util.ArrayList;
import java.util.List;

/**
 *  Interface UML
 */
public class Interface {
    private String id; // xmi:id
    private String name;
    private String visibility;
    private List<Operation> operations;
    private List<Invariant> invariants = new ArrayList<>();

    public Interface(String id, String name, String visibility) {
        this.id = id;
        this.name = name;
        this.visibility = visibility;
        this.operations = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public List<Operation> getOperations() { return operations; }
    public void addOperation(Operation op) { this.operations.add(op); }

    public List<Invariant> getInvariants() { return invariants; }
    public void addInvariant(Invariant inv) { this.invariants.add(inv); }

    @Override
    public String toString() {
        return "Interface{name='" + name + "', operations=" + operations.size() + "}";
    }
}