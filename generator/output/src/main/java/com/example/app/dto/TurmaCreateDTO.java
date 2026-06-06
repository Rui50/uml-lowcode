package com.example.app.dto;

import lombok.*;
import com.example.app.models.Turma;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurmaCreateDTO {

    private int numAlunos;
    private String nome;

    public Turma toEntity() {
        Turma turma = new Turma();
        turma.setNumAlunos(this.getNumAlunos());
        turma.setNome(this.getNome());
        return turma;
    }
}
