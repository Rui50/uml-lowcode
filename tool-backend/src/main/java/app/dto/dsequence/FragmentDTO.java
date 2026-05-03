package app.dto.dsequence;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FragmentDTO {
    // distinguir entre mensagem e frag
    private final String type = "fragment";
    private String operator;
    private List<OperandDTO> operands;
}
