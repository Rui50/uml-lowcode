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
@Table(name = "turno")
public class Turno{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "numero")
    private int numero;

    @Column(name = "capacidade")
    @Builder.Default
    private Integer capacidade = 30;

    // relationship: Turno -> Aluno
    @JsonIgnoreProperties("turno")
    @OneToMany(mappedBy = "turno")
    @Builder.Default
    private List<Aluno> alunos = new ArrayList<>();

    // Validation (para ja so invariantes OCL simples

    /**
     * Validate entity with OCL
     * 
     * - CapacidadePositiva: self.capacidade > 0
     * - MaxCap: self.alunos->size() <= self.capacidade
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
       List<String> errors = new ArrayList<>();

        // Invariant: CapacidadePositiva
        if (!(this.capacidade > 0)) {
            errors.add("Invariant 'CapacidadePositiva' violated.");
        }

        // Invariant: MaxCap
        if (!(this.alunos.size() <= this.capacidade)) {
            errors.add("Invariant 'MaxCap' violated.");
        }

        return errors;
    }

}
