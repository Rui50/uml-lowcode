package app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ClassDTO {
    private String name;
    private String parent;
    private List<AttributeDTO> attributes;
}
