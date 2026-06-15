package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.AlunoTE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoTEResponseDTO {

    private Long id;

    private String empresa;
    private String nome;
    private String email;
    private int numero;

    public static AlunoTEResponseDTO fromEntity(AlunoTE alunoTE) {
        AlunoTEResponseDTO dto = new AlunoTEResponseDTO();
        dto.setId(alunoTE.getId());
        dto.setEmpresa(alunoTE.getEmpresa());
        dto.setNome(alunoTE.getNome());
        dto.setEmail(alunoTE.getEmail());
        dto.setNumero(alunoTE.getNumero());
        return dto;
    }
}
