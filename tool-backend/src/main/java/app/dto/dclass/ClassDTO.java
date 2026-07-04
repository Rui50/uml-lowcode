package app.dto.dclass;

import com.fasterxml.jackson.annotation.JsonProperty;
import generator.spring.SpringEndpointList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ClassDTO {
    private String name;
    private String parent;
    private List<AttributeDTO> attributes;
    private List<OperationDTO> operations;
    // Jackson tira o "is" de getters boolean (isAbstract -> "abstract"); forçamos
    // a chave "isAbstract" para coincidir com o que o frontend lê
    @JsonProperty("isAbstract")
    private boolean isAbstract;
    private List<String> realizations;
    // endpoints REST que esta classe vai expor no target Spring so diz crud e de relações
    private List<SpringEndpointList.Endpoint> endpoints;

    // relações navegáveis DECLARADAS por esta classe (name = campo/role, collection = multivalorada)
    private List<RelationFieldDTO> relations;
}
