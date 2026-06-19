package com.example.app.dto;

import lombok.*;
import com.example.app.models.Aluno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoCreateDTO {

    private int numero;
    private String email;
    private String nome;
    private Long turnoId;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setNumero(this.getNumero());
        aluno.setEmail(this.getEmail());
        aluno.setNome(this.getNome());
        return aluno;
    }
}
