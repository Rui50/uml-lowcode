package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Turno;
import com.example.app.models.Aluno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoResponseDTO {

    private Long id;

    private Integer capacidade;
    private int numero;

    private List<Long> alunosIds;

    public static TurnoResponseDTO fromEntity(Turno turno) {
        TurnoResponseDTO dto = new TurnoResponseDTO();
        dto.setId(turno.getId());
        dto.setCapacidade(turno.getCapacidade());
        dto.setNumero(turno.getNumero());
        dto.setAlunosIds(turno.getAlunos() != null
            ? turno.getAlunos().stream().map(Aluno::getId).collect(Collectors.toList())
            : new ArrayList<>());
        return dto;
    }
}
