package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.LinhaEncomenda;
import com.example.app.models.Produto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinhaEncomendaResponseDTO {

    private Long id;

    private int quantidade;
    private double precoUnitario;

    private Long produtoId;

    public static LinhaEncomendaResponseDTO fromEntity(LinhaEncomenda linhaEncomenda) {
        LinhaEncomendaResponseDTO dto = new LinhaEncomendaResponseDTO();
        dto.setId(linhaEncomenda.getId());
        dto.setQuantidade(linhaEncomenda.getQuantidade());
        dto.setPrecoUnitario(linhaEncomenda.getPrecoUnitario());
        dto.setProdutoId(linhaEncomenda.getProduto() != null
            ? linhaEncomenda.getProduto().getId() : null);
        return dto;
    }
}
