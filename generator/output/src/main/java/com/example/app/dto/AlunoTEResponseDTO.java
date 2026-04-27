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

    private EstadoAluno estado;
    private String nome;
    private String email;
    private int numero;
    private String empresa;

    public static AlunoTEResponseDTO fromEntity(AlunoTE alunoTE) {
        AlunoTEResponseDTO dto = new AlunoTEResponseDTO();
        dto.setId(alunoTE.getId());
        dto.setEstado(alunoTE.getEstado());
        dto.setNome(alunoTE.getNome());
        dto.setEmail(alunoTE.getEmail());
        dto.setNumero(alunoTE.getNumero());
        dto.setEmpresa(alunoTE.getEmpresa());
        return dto;
    }
}
