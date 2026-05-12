package model;

import java.util.ArrayList;
import java.util.List;

/**
 *  Interface UML.
 *  apenas operations — sem atributos, sem herança de classes.
 */
public class Interface {
    private String name;
    private String visibility;
    private List<Operation> operations;

    public Interface(String name, String visibility) {
        this.name = name;
        this.visibility = visibility;
        this.operations = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public List<Operation> getOperations() { return operations; }
    public void addOperation(Operation op) { this.operations.add(op); }

    @Override
    public String toString() {
        return "Interface{name='" + name + "', operations=" + operations.size() + "}";
    }
}