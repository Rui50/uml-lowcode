package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GenerateRequestDTO {
    private AnnotationsDTO annotations;
    private ProjectSettingsDTO settings;
}