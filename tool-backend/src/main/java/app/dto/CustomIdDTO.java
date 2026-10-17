package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// configuração do @Id de uma classe: atributo a usar como id (null/vazio = id Long automático)
@Getter
@Setter
@NoArgsConstructor
public class CustomIdDTO {
    private String attributeName;   // atributo a usar como @Id
    private String attributeType;   // tipo Java desse atributo
}
