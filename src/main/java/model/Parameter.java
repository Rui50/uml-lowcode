package model;

/**
 * Representa um parâmetro dentro de um metodo
 */

public class Parameter {
    private String name;
    private String type;
    private String direction; // in, out, inout

    public Parameter() {
        this.name = "";
        this.type = "";
        this.direction = "in";
    }

    public Parameter(String name, String type, String direction) {
        this.name = name;
        this.type = type;
        this.direction = direction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "Parameter {" +
                "name:'" + name + '\'' +
                ", type:'" + type + '\'' +
                ", direction:'" + direction + '\'' +
                '}';
    }
}
