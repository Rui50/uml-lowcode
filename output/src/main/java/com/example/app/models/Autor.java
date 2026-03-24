package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "autor")
public class Autor{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nacionlidade")
    private String nacionlidade;

    @Column(name = "nome")
    private String nome;

    // relationship: Autor -> Livro
    @JsonIgnoreProperties("autores")
    @ManyToMany(mappedBy = "autores")
    @Builder.Default
    private List<Livro> livros = new ArrayList<>();

}
