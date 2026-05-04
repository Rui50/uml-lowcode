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
}
