package app.dto.dsequence;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageArgumentDTO {
    private String name;
    private String value;
    private String type;
}