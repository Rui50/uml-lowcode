package com.example.app.dto;

import lombok.*;
import com.example.app.models.Turno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCreateDTO {

    private Integer capacidade;
    private int numero;

    public Turno toEntity() {
        Turno turno = new Turno();
        turno.setCapacidade(this.getCapacidade());
        turno.setNumero(this.getNumero());
        return turno;
    }
}
