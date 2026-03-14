package com.example.app.service;

import com.example.app.models.Turma;
import com.example.app.repository.TurmaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;
import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class TurmaService {

    // Dependencies
    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;
    private final TurnoRepository turnoRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Turma> findAll() {
        return turmaRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Turma> findById(Long id) {
        return turmaRepository.findById(id);
    }

    /**
     * @param turma the entity to create
     * @return the created entity
     */
    public Turma create(Turma turma) {
        // init collections for associations
        if (turma.getAlunos() == null) turma.setAlunos(new HashMap<>());
        if (turma.getTurnos() == null) turma.setTurnos(new ArrayList<>());
        // Validate OCL invariants
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turmaRepository.save(turma);
    }

    /**
     * @param id the entity id
     * @param turma the entity data to update
     * @return the updated entity
     */
    public Turma update(Long id, Turma turma) {
        // Find existing entity
        Turma existing = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + id));

        // Update all attributes
        existing.setNome(turma.getNome());


        // para ja coleções nao sao updated aqui

        // Validate OCL invariants
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turmaRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        turmaRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return turmaRepository.existsById(id);
    }

    // Relationship management stuff
    /**
     * Add a alunos to Turma
     * @param id the Turma id
     * @param alunos list of Aluno to add
     * @return the updated Turma
     */
    public Turma addAlunos(Long id, List<Aluno> alunos) {
        Turma turma = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + id));

        // Add all (so it works for batch operation)
        for (Aluno aluno : alunos) {
            turma.getAlunos().put(aluno.getNumero(), aluno);
        }

        // Validate parent entity after adding
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turmaRepository.save(turma);
    }

    /**
     * Remove a Aluno from Turma
     * @param id the Turma id
     * @param alunoIds the ids of the Aluno to remove
     * @return the updated Turma
     */
    public Turma removeAlunos(Long id, List<Integer> alunoIds) {
        Turma turma = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + id));

        // Remove all (batch operation)
        for (Integer alunoId : alunoIds) {
            turma.getAlunos().remove(alunoId);
        }

        return turmaRepository.save(turma);
    }

    /**
     * Add a turnos to Turma
     * @param id the Turma id
     * @param turnos list of Turno to add
     * @return the updated Turma
     */
    public Turma addTurnos(Long id, List<Turno> turnos) {
        Turma turma = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + id));

        // Add all (so it works for batch operation)
        for (Turno turno : turnos) {
            // Validate related entity
            List<String> errors = turno.validate();
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(
                    "Validation failed for Turno: " + errors
                );
            }

            turma.getTurnos().add(turno);
        }

        // Validate parent entity after adding
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turmaRepository.save(turma);
    }

    /**
     * Remove a Turno from Turma
     * @param id the Turma id
     * @param turnoIds the ids of the Turno to remove
     * @return the updated Turma
     */
    public Turma removeTurnos(Long id, List<Long> turnoIds) {
        Turma turma = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + id));

        // Remove all (batch operation)
        turma.getTurnos().removeIf(turno -> turnoIds.contains(turno.getId()));

        return turmaRepository.save(turma);
    }

    // Simple query methods for strings
    /**
     * Find Turma by nome
     * @param nome to search by
     * @return list of matching entities
     */
    public List<Turma> findByNome(String nome) {
        return turmaRepository.findByNome(nome);
    }

    /**
     * Find Turma by nome containing text
     * @param nome to search
     * @return list of matching entities
     */
    public List<Turma> findByNomeContaining(String nome) {
        return turmaRepository.findByNomeContaining(nome);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @param alunos List<Aluno>
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public void inicializarAlunosParaTeste(Long id, List<Aluno> alunos) {
        throw new UnsupportedOperationException(
            "Method 'inicializarAlunosParaTeste' not yet implemented. "
        );
    }

    /**
     * @param destino Turma
     * @param numero int
     * @return boolean
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public boolean transferirAluno(Long id, Turma destino, int numero) {
        throw new UnsupportedOperationException(
            "Method 'transferirAluno' not yet implemented. "
        );
    }

    /**
     * @return int
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public int totalAlunos(Long id) {
        throw new UnsupportedOperationException(
            "Method 'totalAlunos' not yet implemented. "
        );
    }

}
