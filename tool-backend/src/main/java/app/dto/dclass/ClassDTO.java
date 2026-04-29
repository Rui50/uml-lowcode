package app.dto.dclass;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ClassDTO {
    private String name;
    private String parent;
    private List<AttributeDTO> attributes;
    private List<String> operations;
}
