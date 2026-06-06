package com.example.app.dto;

import lombok.*;
import com.example.app.models.Turno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCreateDTO {

    private int numero;
    private Integer capacidade;

    public Turno toEntity() {
        Turno turno = new Turno();
        turno.setNumero(this.getNumero());
        turno.setCapacidade(this.getCapacidade());
        return turno;
    }
}
