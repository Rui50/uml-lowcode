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
@Table(name = "leitor")
public class Leitor{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nome")
    private String nome;

    @Column(name = "numCartao")
    private int numCartao;

    @Column(name = "email")
    private String email;

    // relationship: Leitor -> Emprestimo
    @OneToMany
    @JoinColumn(name = "leitor_id")
    @Builder.Default
    private List<Emprestimo> emprestimos = new ArrayList<>();

}
