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
@Table(name = "turno")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "capacidade")
    private int capacidade;

    // relationship: Turno -> Aluno
    @JsonIgnoreProperties("turno")
    @OneToMany(mappedBy = "turno")
    @Builder.Default
    private List<Aluno> alunos = new ArrayList<>();

}
