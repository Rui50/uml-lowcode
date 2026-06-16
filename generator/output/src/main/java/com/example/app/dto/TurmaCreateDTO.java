package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Turma;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurmaCreateDTO {

    private String nome;
    private List<Long> alunosIds;
    private List<Long> turnosIds;

    public Turma toEntity() {
        Turma turma = new Turma();
        turma.setNome(this.getNome());
        return turma;
    }
}
