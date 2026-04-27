package model.sequence;

import java.util.ArrayList;
import java.util.List;

public class Operand {
    private String id;
    private String guard;
    private List<Object> elements;  // Message ou Fragment, por ordem

    public Operand() {
        this.elements = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGuard() {
        return guard;
    }

    public void setGuard(String guard) {
        this.guard = guard;
    }

    public List<Object> getElements() {
        return elements;
    }

    public void addElement(Object element) {
        this.elements.add(element);
    }

    @Override
    public String toString() {
        return "Operand{" +
                "guard='" + guard + '\'' +
                ", elements=" + elements +
                '}';
    }
}