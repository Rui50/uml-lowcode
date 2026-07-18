package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class AnnotationsDTO {
    // key classId (xmi:id)
    private Map<String, List<OclDTO>> invariantsByClass = new HashMap<>();

    // key "classId.attrName"
    private Map<String, String> initByAttribute = new HashMap<>();
    private Map<String, String> deriveByAttribute = new HashMap<>();

    // key operationId
    private Map<String, String> operationToSeqDiagram = new HashMap<>();

    // key operationId
    private Map<String, List<OclDTO>> preconditionsByOperation = new HashMap<>();

    private Map<String, List<OclDTO>> postconditionsByOperation = new HashMap<>();

    // key classId (xmi:id, superclasse/raiz da hierarquia) -> "SINGLE_TABLE" | "JOINED"
    private Map<String, String> inheritanceStrategyByClass = new HashMap<>();

    // key classId (xmi:id, raiz da hierarquia) -> config do @Id
    private Map<String, CustomIdDTO> idByClass = new HashMap<>();

    // key operationId -> config do endpoint REST / job
    private Map<String, EndpointConfigDTO> endpointByOperation = new HashMap<>();

    // key classId (xmi:id) -> gerar controller REST? (Spring; default true)
    private Map<String, Boolean> generateControllerByClass = new HashMap<>();

    // key classId (xmi:id) -> invariantes OCL viram anotações Bean Validation? (Spring; default true)
    private Map<String, Boolean> beanValidationByClass = new HashMap<>();

    // key classId (xmi:id) -> override do nome da tabela JPA (vazio = nome derivado da classe)
    private Map<String, String> tableNameByClass = new HashMap<>();

    // key classId (xmi:id) -> atributos escondidos por DTO (ausente = DTOs completos)
    private Map<String, HiddenFieldsDTO> dtoHiddenByClass = new HashMap<>();

    // key classId (xmi:id) -> endpointType ("List"|"Get"|"Create"|"Update") -> override do response DESSE
    // endpoint (nome + escondidos; entrada presente = override ativo; ausente = ResponseDTO da classe)
    private Map<String, Map<String, ResponseOverrideDTO>> responseOverrideByClass = new HashMap<>();

    // key "classId.attrName" -> config de coluna JPA (persistence tab sem nada = @Column só com name-default)
    private Map<String, ColumnConfigDTO> columnByAttribute = new HashMap<>();

    // key "classId.relationName" -> @OrderBy da coleção (ausente = sem ordenação)
    private Map<String, OrderByDTO> orderByCollection = new HashMap<>();
}
