package com.example.app.dto;

import lombok.*;
import com.example.app.models.AlunoTE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoTECreateDTO {

    private String empresa;
    private String nome;
    private String email;
    private int numero;

    public AlunoTE toEntity() {
        AlunoTE alunoTE = new AlunoTE();
        alunoTE.setEmpresa(this.getEmpresa());
        alunoTE.setNome(this.getNome());
        alunoTE.setEmail(this.getEmail());
        alunoTE.setNumero(this.getNumero());
        return alunoTE;
    }
}
