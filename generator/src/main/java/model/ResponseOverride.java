package model;

import java.util.Set;

// override do response de um endpoint: nome do DTO gerado (vazio = auto <Class><Slot>ResponseDTO)
// e atributos escondidos nesse endpoint.
public class ResponseOverride {
    private final String name;
    private final Set<String> hidden;

    public ResponseOverride(String name, Set<String> hidden) {
        this.name = name;
        this.hidden = hidden;
    }

    public String getName() { return name; }
    public Set<String> getHidden() { return hidden; }
}
