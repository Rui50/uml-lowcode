package app.service;

import app.dto.ParseResponse;
import app.dto.dclass.AssociationDTO;
import app.dto.dclass.AttributeDTO;
import app.dto.dclass.ClassDTO;
import app.dto.dsequence.InteractionDTO;
import app.dto.dsequence.LifelineDTO;
import app.dto.dsequence.MessageArgumentDTO;
import app.dto.dsequence.MessageDTO;
import model.Association;
import model.Attribute;
import model.Class;
import model.ParsedModel;
import model.sequence.Interaction;
import model.sequence.Message;
import model.sequence.MessageArgument;
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

        List<InteractionDTO> interactions = model.getPimInteractions().stream()
                .map(this::toInteractionDTO)
                .toList();

        return new ParseResponse(classes, associations, interactions);
    }

    /**
     * MAPPERS DEPOIS PODEM SER MOVIDOS

     */
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

    private InteractionDTO toInteractionDTO(Interaction i){
        List<LifelineDTO> lifelines = i.getLifelines().stream().map(this::toLifelineDTO).toList();
        List<MessageDTO> messages = i.getMessages().stream().map(this::toMessageDTO).toList();
        return new InteractionDTO(i.getId(), i.getName(), i.getContextClass(), lifelines, messages);
    }

    private LifelineDTO toLifelineDTO(model.sequence.Lifeline l){
        return new LifelineDTO(l.getId(), l.getName(), l.getRepresents(), l.getType());
    }

    private MessageDTO toMessageDTO(Message m) {
        List<MessageArgumentDTO> args = m.getArguments().stream()
                .map(this::toMessageArgumentDTO)
                .toList();
        return new MessageDTO(
                m.getId(),
                m.getName(),
                m.getRawName(),
                m.getMessageSort(),
                m.getReturnType(),
                m.getAssignVariable(),
                m.getSource() != null ? m.getSource().getId() : null,
                m.getTarget() != null ? m.getTarget().getId() : null,
                m.getSequenceNumber(),
                m.getVpNumber(),
                args
        );
    }

    private MessageArgumentDTO toMessageArgumentDTO(MessageArgument a) {
        return new MessageArgumentDTO(a.getName(), a.getValue(), a.getType());
    }


}