package app.dto.dclass;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OperationDTO {
    private String id;
    private String name;
    private String returnType;
    private String visibility;
    private List<ParameterDTO> parameters;

}
