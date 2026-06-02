package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.ImplementInterface;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImplementInterfaceResponseDTO {

    private Long id;


    public static ImplementInterfaceResponseDTO fromEntity(ImplementInterface implementInterface) {
        ImplementInterfaceResponseDTO dto = new ImplementInterfaceResponseDTO();
        dto.setId(implementInterface.getId());
        return dto;
    }
}
