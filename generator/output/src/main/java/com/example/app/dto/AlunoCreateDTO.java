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
    private int numero;
    private String nome;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setEmail(this.getEmail());
        aluno.setEstado(this.getEstado());
        aluno.setNumero(this.getNumero());
        aluno.setNome(this.getNome());
        return aluno;
    }
}
