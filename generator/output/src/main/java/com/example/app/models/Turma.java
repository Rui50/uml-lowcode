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
@Table(name = "turma")
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nome")
    private String nome;

    // relationship: Turma -> Aluno
    @OneToMany
    @JoinColumn(name = "turma_id")
    @Builder.Default
    @MapKey(name = "numero")
    private Map<Integer, Aluno> alunos = new HashMap<>();

    // relationship: Turma -> Turno
    @OneToMany
    @JoinColumn(name = "turma_id")
    @Builder.Default
    private List<Turno> turnos = new ArrayList<>();

}
