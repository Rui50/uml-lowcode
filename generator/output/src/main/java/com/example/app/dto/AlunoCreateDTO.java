package com.example.app.dto;

import lombok.*;
import com.example.app.models.Aluno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoCreateDTO {

    private String nome;
    private String email;
    private int numero;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setNome(this.getNome());
        aluno.setEmail(this.getEmail());
        aluno.setNumero(this.getNumero());
        return aluno;
    }
}
