package app.dto;

import app.dto.dclass.AssociationDTO;
import app.dto.dclass.ClassDTO;
import app.dto.dclass.EnumerationDTO;
import app.dto.dclass.InterfaceDTO;
import app.dto.dsequence.InteractionDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ParseResponse {
    private List<ClassDTO> classes;
    private List<AssociationDTO> associations;
    private List<InteractionDTO> interactions;
    private List<EnumerationDTO> enumerations;
    private List<InterfaceDTO> interfaces;
}
