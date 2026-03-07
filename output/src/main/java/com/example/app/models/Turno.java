package com.example.app.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "turno")
public class Turno{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "capacidade")
    private int capacidade;

    // relationship: Turno -> Aluno
    @OneToMany(mappedBy = "turno")
    @Builder.Default
    private List<Aluno> alunos = new ArrayList<>();

}
