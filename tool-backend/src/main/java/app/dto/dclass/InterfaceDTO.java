package app.dto.dclass;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InterfaceDTO {
    private String name;
    private List<OperationDTO> operations;


}
