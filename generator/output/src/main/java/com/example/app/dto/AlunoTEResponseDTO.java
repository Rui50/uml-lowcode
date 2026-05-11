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
    private String email;
    private EstadoAluno estado;
    private String nome;
    private int numero;

    public static AlunoTEResponseDTO fromEntity(AlunoTE alunoTE) {
        AlunoTEResponseDTO dto = new AlunoTEResponseDTO();
        dto.setId(alunoTE.getId());
        dto.setEmpresa(alunoTE.getEmpresa());
        dto.setEmail(alunoTE.getEmail());
        dto.setEstado(alunoTE.getEstado());
        dto.setNome(alunoTE.getNome());
        dto.setNumero(alunoTE.getNumero());
        return dto;
    }
}
