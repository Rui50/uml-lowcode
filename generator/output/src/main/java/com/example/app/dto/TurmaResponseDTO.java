package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Turma;
import com.example.app.models.Aluno;
import com.example.app.models.Turno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurmaResponseDTO {

    private Long id;

    private String nome;

    private List<Long> alunosIds;
    private List<Long> turnosIds;

    public static TurmaResponseDTO fromEntity(Turma turma) {
        TurmaResponseDTO dto = new TurmaResponseDTO();
        dto.setId(turma.getId());
        dto.setNome(turma.getNome());
        dto.setAlunosIds(turma.getAlunos() != null
            ? turma.getAlunos().values().stream().map(Aluno::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setTurnosIds(turma.getTurnos() != null
            ? turma.getTurnos().stream().map(Turno::getId).collect(Collectors.toList())
            : new ArrayList<>());
        return dto;
    }
}
