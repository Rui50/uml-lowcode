package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Turno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCreateDTO {

    private int capacidade;
    private int numero;
    private List<Long> alunosIds;

    public Turno toEntity() {
        Turno turno = new Turno();
        turno.setCapacidade(this.getCapacidade());
        turno.setNumero(this.getNumero());
        return turno;
    }
}
