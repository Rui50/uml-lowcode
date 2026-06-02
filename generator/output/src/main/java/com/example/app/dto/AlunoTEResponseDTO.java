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
    private int numero;
    private String nome;

    public static AlunoTEResponseDTO fromEntity(AlunoTE alunoTE) {
        AlunoTEResponseDTO dto = new AlunoTEResponseDTO();
        dto.setId(alunoTE.getId());
        dto.setEmpresa(alunoTE.getEmpresa());
        dto.setEmail(alunoTE.getEmail());
        dto.setEstado(alunoTE.getEstado());
        dto.setNumero(alunoTE.getNumero());
        dto.setNome(alunoTE.getNome());
        return dto;
    }
}
