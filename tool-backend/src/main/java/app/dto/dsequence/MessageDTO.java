package app.dto.dsequence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String name;             // method name only
    private String rawName;          // full original signature
    private String messageSort;      // synchCall, asynchCall, reply
    private String returnType;
    private String assignVariable;
    private String sourceLifelineId;
    private String targetLifelineId;
    private int sequenceNumber;
    private String vpNumber;
    private List<MessageArgumentDTO> arguments;
}