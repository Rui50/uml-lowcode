package app.dto.dsequence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class InteractionDTO {
    private String id;
    private String name;
    private String contextClass;
    private List<LifelineDTO> lifelines;
    private List<MessageDTO> messages;
}
