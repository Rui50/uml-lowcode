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
    @JoinColumn(name = "turma_id")
    @Builder.Default
    @MapKey(name = "numero")
    private Map<Integer, Aluno> alunos = new HashMap<>();

    // relationship: Turma -> Turno
    @OneToMany
    @JoinColumn(name = "turma_id")
    @Builder.Default
    private List<Turno> turnos = new ArrayList<>();

    // ========================================================================
    // Validation (para ja so invariantes OCL simples
    // ========================================================================

    /**
     * Validate entity against OCL invariants
     * 
     * - MaxAlunos: self.alunos -> size() <= 30
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
       List<String> errors = new ArrayList<>();

        // Invariant: MaxAlunos
        if (!(alunos.size() <= 30)) {
            errors.add("Invariant 'MaxAlunos' violated.");
        }

        return errors;
    }

}
