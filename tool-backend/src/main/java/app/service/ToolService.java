package app.service;

import app.dto.*;
import app.dto.AssociationDTO;
import model.Association;
import model.Attribute;
import model.Class;
import model.ParsedModel;
import org.springframework.stereotype.Service;
import service.ParsingService;

import java.util.List;

@Service
public class ToolService {
    private final ParsingService parsingService = new ParsingService();

    public ParseResponse parse(String xmiFile) throws Exception{
        ParsedModel model = parsingService.parse(xmiFile);
        return toResponse(model);
        
    }

    private ParseResponse toResponse(ParsedModel model) {
        List<ClassDTO> classes = model.getClasses().stream()
                .map(this::toClassDto)
                .toList();

        List<AssociationDTO> associations = model.getAssociations().stream()
                .map(this::toAssociationDto)
                .toList();

        return new ParseResponse(classes, associations);
    }

    private ClassDTO toClassDto(Class c) {
        String parent = c.getParentClass() != null ? c.getParentClass().getName() : null;
        List<AttributeDTO> attrs = c.getAttributes().stream()
                .map(this::toAttributeDto)
                .toList();
        return new ClassDTO(c.getName(), parent, attrs);
    }

    private AttributeDTO toAttributeDto(Attribute a) {
        return new AttributeDTO(a.getName(), a.getType());
    }

    private AssociationDTO toAssociationDto(Association a) {
        return new AssociationDTO(
                a.getSourceClass(),         // from Relationship
                a.getTargetClass(),         // from Relationship
                a.getSourceMultiplicity(),
                a.getTargetMultiplicity(),
                a.getSourceRole(),
                a.getTargetRole()
        );
    }
}