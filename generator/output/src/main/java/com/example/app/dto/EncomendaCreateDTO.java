package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Encomenda;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncomendaCreateDTO {

    private String data;
    private String estado;
    private List<Long> linhasIds;

    public Encomenda toEntity() {
        Encomenda encomenda = new Encomenda();
        encomenda.setData(this.getData());
        encomenda.setEstado(this.getEstado());
        return encomenda;
    }
}
