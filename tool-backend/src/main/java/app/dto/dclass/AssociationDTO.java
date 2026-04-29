package app.dto.dclass;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssociationDTO {
    private String source;
    private String target;

    private String sourceMultiplicity;
    private String targetMultiplicity;

    private String sourceRole;
    private String targetRole;

    private boolean sourceNavigable;
    private boolean targetNavigable;
}
