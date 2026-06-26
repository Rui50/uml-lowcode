package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Encomenda;
import com.example.app.models.LinhaEncomenda;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncomendaResponseDTO {

    private Long id;

    private String data;
    private String estado;

    private List<Long> linhasIds;

    public static EncomendaResponseDTO fromEntity(Encomenda encomenda) {
        EncomendaResponseDTO dto = new EncomendaResponseDTO();
        dto.setId(encomenda.getId());
        dto.setData(encomenda.getData());
        dto.setEstado(encomenda.getEstado());
        dto.setLinhasIds(encomenda.getLinhas() != null
            ? encomenda.getLinhas().stream().map(LinhaEncomenda::getId).collect(Collectors.toList())
            : new ArrayList<>());
        return dto;
    }
}
