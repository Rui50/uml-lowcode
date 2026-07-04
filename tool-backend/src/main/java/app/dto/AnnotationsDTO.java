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
    // key className
    private Map<String, List<OclDTO>> invariantsByClass = new HashMap<>();

    // key "ClassName.attrName"
    private Map<String, String> initByAttribute = new HashMap<>();
    private Map<String, String> deriveByAttribute = new HashMap<>();

    private Map<String, String> operationToSeqDiagram = new HashMap<>();

    // key "ClassName.opName"
    private Map<String, List<OclDTO>> preconditionsByOperation = new HashMap<>();

    private Map<String, List<OclDTO>> postconditionsByOperation = new HashMap<>();

    // key className (superclasse, raiz da hierarquia) -> "SINGLE_TABLE" | "JOINED"
    private Map<String, String> inheritanceStrategyByClass = new HashMap<>();

    // key className (raiz da hierarquia) -> config do @Id
    private Map<String, CustomIdDTO> idByClass = new HashMap<>();

    // key operationId -> config do endpoint REST / job
    private Map<String, EndpointConfigDTO> endpointByOperation = new HashMap<>();

    // key className -> gerar controller REST? (Spring; default true)
    private Map<String, Boolean> generateControllerByClass = new HashMap<>();

    // key className -> invariantes OCL viram anotações Bean Validation? (Spring; default true)
    private Map<String, Boolean> beanValidationByClass = new HashMap<>();

    // key className -> override do nome da tabela JPA (vazio = nome derivado da classe)
    private Map<String, String> tableNameByClass = new HashMap<>();

    // key className -> atributos escondidos por DTO (ausente = DTOs completos)
    private Map<String, HiddenFieldsDTO> dtoHiddenByClass = new HashMap<>();

    // key className -> endpointType ("list"|"get"|"create"|"update") -> override do response DESSE
    // endpoint (nome + escondidos; entrada presente = override ativo; ausente = ResponseDTO da classe)
    private Map<String, Map<String, ResponseOverrideDTO>> responseOverrideByClass = new HashMap<>();
}
