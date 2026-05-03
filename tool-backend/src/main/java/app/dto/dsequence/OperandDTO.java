package app.dto.dsequence;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OperandDTO {
    private String guard;
    private List<Object> elements; // message or frag
}
