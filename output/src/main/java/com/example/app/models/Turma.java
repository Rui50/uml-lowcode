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
@Table(name = "turma")
public class Turma{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nome")
    private String nome;

    // relationship: Turma -> Aluno
    @OneToMany
    @JoinColumn(name = "aluno_id")
    @MapKey(name = "aluno_num")
    private Map<Integer, Aluno> alunoss = new HashMap<>();

    // relationship: Turma -> Turno
    @OneToMany
    @JoinColumn(name = "turma_id")
    private List<Turno> turnos = new ArrayList<>();

}
