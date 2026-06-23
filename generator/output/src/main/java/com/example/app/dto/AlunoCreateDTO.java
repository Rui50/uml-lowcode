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

    private int numero;
    private String email;
    private String nome;
    private EstadoAluno estado;
    private Long turnoId;

    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setNumero(this.getNumero());
        aluno.setEmail(this.getEmail());
        aluno.setNome(this.getNome());
        aluno.setEstado(this.getEstado());
        return aluno;
    }
}
