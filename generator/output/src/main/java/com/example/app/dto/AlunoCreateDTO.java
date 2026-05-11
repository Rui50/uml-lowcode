package com.example.app.dto;

import lombok.*;
import com.example.app.models.Aluno;
import com.example.app.models.EstadoAluno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoCreateDTO {

    private String email;
    private EstadoAluno estado;
    private String nome;
    private int numero;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setEmail(this.getEmail());
        aluno.setEstado(this.getEstado());
        aluno.setNome(this.getNome());
        aluno.setNumero(this.getNumero());
        return aluno;
    }
}
