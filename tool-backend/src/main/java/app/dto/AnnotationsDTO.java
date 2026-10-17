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
    // classId (xmi:id)
    private Map<String, List<OclDTO>> invariantsByClass = new HashMap<>();

    // classId -> (attrName -> expressão)
    private Map<String, Map<String, String>> initByAttribute = new HashMap<>();
    private Map<String, Map<String, String>> deriveByAttribute = new HashMap<>();

    // operationId
    private Map<String, String> operationToSeqDiagram = new HashMap<>();

    // operationId
    private Map<String, List<OclDTO>> preconditionsByOperation = new HashMap<>();

    private Map<String, List<OclDTO>> postconditionsByOperation = new HashMap<>();

    // classId (xmi:id, superclasse/raiz da hierarquia) -> "SINGLE_TABLE" | "JOINED"
    private Map<String, String> inheritanceStrategyByClass = new HashMap<>();

    // classId (xmi:id, raiz da hierarquia) -> config do @Id
    private Map<String, CustomIdDTO> idByClass = new HashMap<>();

    // operationId -> config do endpoint REST / job
    private Map<String, EndpointConfigDTO> endpointByOperation = new HashMap<>();

    // classId (xmi:id) -> gerar controller REST? (Spring; default true)
    private Map<String, Boolean> generateControllerByClass = new HashMap<>();

    // classId (xmi:id) -> invariantes OCL viram anotações Bean Validation? (Spring; default true)
    private Map<String, Boolean> beanValidationByClass = new HashMap<>();

    // classId (xmi:id) -> override do nome da tabela JPA (vazio = nome derivado da classe)
    private Map<String, String> tableNameByClass = new HashMap<>();

    // classId (xmi:id) -> atributos escondidos por DTO (ausente = DTOs completos)
    private Map<String, HiddenFieldsDTO> dtoHiddenByClass = new HashMap<>();

    // classId (xmi:id) -> endpointType ("List"|"Get"|"Create"|"Update") -> override do response DESSE
    // endpoint (nome + escondidos, entrada presente = override ativo, ausente = ResponseDTO da classe)
    private Map<String, Map<String, ResponseOverrideDTO>> responseOverrideByClass = new HashMap<>();

    // classId -> (attrName -> config de coluna JPA), sem nada = @Column só com name-default
    private Map<String, Map<String, ColumnConfigDTO>> columnByAttribute = new HashMap<>();

    // classId -> (relationName -> @OrderBy da coleção), ausente = sem ordenação
    private Map<String, Map<String, OrderByDTO>> orderByCollection = new HashMap<>();

    // classId -> (relationName -> fetch de uma rel singular "LAZY"|"EAGER"), ausente = LAZY
    private Map<String, Map<String, String>> fetchByRelation = new HashMap<>();
}
