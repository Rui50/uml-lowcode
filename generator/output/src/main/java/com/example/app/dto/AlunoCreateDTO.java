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
    private int numero;
    private String email;
    private Long turnoId;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setNome(this.getNome());
        aluno.setNumero(this.getNumero());
        aluno.setEmail(this.getEmail());
        return aluno;
    }
}
