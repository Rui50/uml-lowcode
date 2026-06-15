package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Aluno;
import com.example.app.models.Turno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoResponseDTO {

    private Long id;

    private String nome;
    private String email;
    private int numero;

    private Long turnoId;

    public static AlunoResponseDTO fromEntity(Aluno aluno) {
        AlunoResponseDTO dto = new AlunoResponseDTO();
        dto.setId(aluno.getId());
        dto.setNome(aluno.getNome());
        dto.setEmail(aluno.getEmail());
        dto.setNumero(aluno.getNumero());
        dto.setTurnoId(aluno.getTurno() != null
            ? aluno.getTurno().getId() : null);
        return dto;
    }
}
