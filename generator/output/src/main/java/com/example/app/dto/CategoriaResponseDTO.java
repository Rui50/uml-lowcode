package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Categoria;
import com.example.app.models.Produto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaResponseDTO {

    private Long id;

    private String nome;
    private String descricao;

    private Long produtoId;

    public static CategoriaResponseDTO fromEntity(Categoria categoria) {
        CategoriaResponseDTO dto = new CategoriaResponseDTO();
        dto.setId(categoria.getId());
        dto.setNome(categoria.getNome());
        dto.setDescricao(categoria.getDescricao());
        dto.setProdutoId(categoria.getProduto() != null
            ? categoria.getProduto().getId() : null);
        return dto;
    }
}
