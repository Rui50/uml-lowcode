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
    private int numero;
    private String email;
    private String nome;

    public AlunoTE toEntity() {
        AlunoTE alunoTE = new AlunoTE();
        alunoTE.setEmpresa(this.getEmpresa());
        alunoTE.setNumero(this.getNumero());
        alunoTE.setEmail(this.getEmail());
        alunoTE.setNome(this.getNome());
        return alunoTE;
    }
}
