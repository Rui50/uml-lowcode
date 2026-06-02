package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.testeSubAb;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class testeSubAbResponseDTO {

    private Long id;

    private String nada;
    private String testar;

    public static testeSubAbResponseDTO fromEntity(testeSubAb testeSubAb) {
        testeSubAbResponseDTO dto = new testeSubAbResponseDTO();
        dto.setId(testeSubAb.getId());
        dto.setNada(testeSubAb.getNada());
        dto.setTestar(testeSubAb.getTestar());
        return dto;
    }
}
