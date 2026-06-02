package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@Entity
@Table(name = "aluno")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("Aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoAluno estado;

    @Column(name = "numero")
    private int numero;

    @Column(name = "nome")
    private String nome;

    // relationship: Aluno -> Turno
    @JsonIgnoreProperties("alunos")
    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;

}
