package app.dto.dclass;

import generator.spring.SpringEndpointList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ClassDTO {
    private String id; // xmi:id
    private String name;
    private String parent;
    private List<AttributeDTO> attributes;
    private List<OperationDTO> operations;
    // o Jackson mete isto como "abstract": tira o "is" do getter
    private boolean isAbstract;
    private List<String> realizations;
    // endpoints REST que esta classe vai expor no target Spring so diz crud e de relações
    private List<SpringEndpointList.Endpoint> endpoints;

    // relações navegáveis DECLARADAS por esta classe (name = campo/role, collection = multivalorada)
    private List<RelationFieldDTO> relations;
}
