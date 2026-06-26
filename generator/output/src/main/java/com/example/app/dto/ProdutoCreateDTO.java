package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Produto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoCreateDTO {

    private String nome;
    private double preco;
    private int stock;

    public Produto toEntity() {
        Produto produto = new Produto();
        produto.setNome(this.getNome());
        produto.setPreco(this.getPreco());
        produto.setStock(this.getStock());
        return produto;
    }
}
