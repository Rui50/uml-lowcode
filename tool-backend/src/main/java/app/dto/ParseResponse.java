package app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ParseResponse {
    private List<ClassDTO> classes;
    private List<AssociationDTO> associations;
}
