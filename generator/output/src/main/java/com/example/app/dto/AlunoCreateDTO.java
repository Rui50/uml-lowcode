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

    private EstadoAluno estado;
    private String nome;
    private String email;
    private int numero;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        if (this.getEstado() != null) aluno.setEstado(this.getEstado());
        aluno.setNome(this.getNome());
        aluno.setEmail(this.getEmail());
        aluno.setNumero(this.getNumero());
        return aluno;
    }
}
