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
@Table(name = "exemplar")
public class Exemplar{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "disponivel")
    private boolean disponivel;

    // relationship: Exemplar -> Emprestimo
    @OneToMany
    @JoinColumn(name = "exemplar_id")
    @Builder.Default
    private List<Emprestimo> emprestimos = new ArrayList<>();

}
