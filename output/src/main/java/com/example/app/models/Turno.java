package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties("turno")
    @OneToMany(mappedBy = "turno")
    @Builder.Default
    private List<Aluno> alunos = new ArrayList<>();

    // ========================================================================
    // Validation (para ja so invariantes OCL simples
    // ========================================================================

    /**
     * Validate entity against OCL invariants
     * 
     * - CapacidadePositiva: self.capacidade > 0
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
       List<String> errors = new ArrayList<>();

        // Invariant: CapacidadePositiva
        if (!(capacidade > 0)) {
            errors.add("Invariant 'CapacidadePositiva' violated.");
        }

        return errors;
    }

}
