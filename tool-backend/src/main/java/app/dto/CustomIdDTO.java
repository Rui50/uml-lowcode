package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// configuração do @Id de uma classe (sempre auto-gerado pela BD)
@Getter
@Setter
@NoArgsConstructor
public class CustomIdDTO {
    private String attributeName;   // atributo a usar como @Id
    private String attributeType;   // tipo Java desse atributo
}
