package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.LinhaEncomenda;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinhaEncomendaCreateDTO {

    private int quantidade;
    private double precoUnitario;
    private Long produtoId;

    public LinhaEncomenda toEntity() {
        LinhaEncomenda linhaEncomenda = new LinhaEncomenda();
        linhaEncomenda.setQuantidade(this.getQuantidade());
        linhaEncomenda.setPrecoUnitario(this.getPrecoUnitario());
        return linhaEncomenda;
    }
}
