package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.AlunoTE;
import com.example.app.models.EstadoAluno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoTEResponseDTO {

    private Long id;

    private String empresa;
    private int numero;
    private String email;
    private String nome;
    private EstadoAluno estado;

    public static AlunoTEResponseDTO fromEntity(AlunoTE alunoTE) {
        AlunoTEResponseDTO dto = new AlunoTEResponseDTO();
        dto.setId(alunoTE.getId());
        dto.setEmpresa(alunoTE.getEmpresa());
        dto.setNumero(alunoTE.getNumero());
        dto.setEmail(alunoTE.getEmail());
        dto.setNome(alunoTE.getNome());
        dto.setEstado(alunoTE.getEstado());
        return dto;
    }
}
