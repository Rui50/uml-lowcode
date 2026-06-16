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
    private int numero;
    private String email;

    public AlunoTE toEntity() {
        AlunoTE alunoTE = new AlunoTE();
        alunoTE.setEmpresa(this.getEmpresa());
        alunoTE.setNome(this.getNome());
        alunoTE.setNumero(this.getNumero());
        alunoTE.setEmail(this.getEmail());
        return alunoTE;
    }
}
