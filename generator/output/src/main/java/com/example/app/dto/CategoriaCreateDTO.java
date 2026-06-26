package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Categoria;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaCreateDTO {

    private String nome;
    private String descricao;
    private Long produtoId;

    public Categoria toEntity() {
        Categoria categoria = new Categoria();
        categoria.setNome(this.getNome());
        categoria.setDescricao(this.getDescricao());
        return categoria;
    }
}
