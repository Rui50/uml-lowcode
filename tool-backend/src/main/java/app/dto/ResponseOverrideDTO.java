package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// override do response de um endpoint: nome do DTO (vazio = auto) + atributos escondidos
@Getter
@Setter
@NoArgsConstructor
public class ResponseOverrideDTO {
    private String name = "";
    private List<String> hidden = new ArrayList<>();
}
