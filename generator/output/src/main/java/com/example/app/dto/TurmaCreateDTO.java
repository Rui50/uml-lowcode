package com.example.app.dto;

import lombok.*;
import com.example.app.models.Turma;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurmaCreateDTO {

    private String nome;
    private int numAlunos;

    public Turma toEntity() {
        Turma turma = new Turma();
        turma.setNome(this.getNome());
        turma.setNumAlunos(this.getNumAlunos());
        return turma;
    }
}
