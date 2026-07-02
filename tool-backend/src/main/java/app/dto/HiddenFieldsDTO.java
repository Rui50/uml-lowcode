package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// atributos escondidos de cada DTO gerado (Create/Update/Response) de uma classe
@Getter
@Setter
@NoArgsConstructor
public class HiddenFieldsDTO {
    private List<String> create = new ArrayList<>();
    private List<String> update = new ArrayList<>();
    private List<String> response = new ArrayList<>();
}
