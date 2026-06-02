package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.testeClassYes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class testeClassYesResponseDTO {

    private Long id;

    private String testar;

    public static testeClassYesResponseDTO fromEntity(testeClassYes testeClassYes) {
        testeClassYesResponseDTO dto = new testeClassYesResponseDTO();
        dto.setId(testeClassYes.getId());
        dto.setTestar(testeClassYes.getTestar());
        return dto;
    }
}
