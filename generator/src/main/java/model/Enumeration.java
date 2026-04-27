package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma enumeração UML.
 */
public class Enumeration {
    private String name;
    private List<String> literals;

    public Enumeration(String name) {
        this.name = name;
        this.literals = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLiterals() {
        return literals;
    }

    public void addLiteral(String literal) {
        this.literals.add(literal);
    }

    @Override
    public String toString() {
        return "Enumeration {" +
                "name:'" + name + '\'' +
                ", literals:" + literals +
                '}';
    }
}
