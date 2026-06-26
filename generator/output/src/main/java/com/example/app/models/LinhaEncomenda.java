package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "linhaencomenda")
public class LinhaEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "quantidade")
    private int quantidade;

    @Column(name = "precoUnitario")
    private double precoUnitario;

    // relationship: LinhaEncomenda -> Produto
    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

}
