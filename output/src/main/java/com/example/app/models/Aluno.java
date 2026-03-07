package com.example.app.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "aluno")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("Aluno")
public class Aluno{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nome")
    private String nome;

    @Column(name = "email")
    private String email;

    @Column(name = "numero")
    private int numero;

    // relationship: Aluno -> Turno
    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;

}
