package com.example.app.service;

import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class TurnoService {

    // Dependencies
    private final TurnoRepository turnoRepository;
    private final AlunoRepository alunoRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Turno> findAll() {
        return turnoRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Turno> findById(Long id) {
        return turnoRepository.findById(id);
    }

    /**
     * @param turno the entity to create
     * @return the created entity
     */
    public Turno create(Turno turno) {
        // init collections for associations
        if (turno.getAlunos() == null) turno.setAlunos(new ArrayList<>());
        // Validate OCL invariants
        List<String> errors = turno.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turnoRepository.save(turno);
    }

    /**
     * @param id the entity id
     * @param turno the entity data to update
     * @return the updated entity
     */
    public Turno update(Long id, Turno turno) {
        // Find existing entity
        Turno existing = turnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turno not found: " + id));

        // Update all attributes
        existing.setCapacidade(turno.getCapacidade());


        // para ja coleções nao sao updated aqui

        // Validate OCL invariants
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turnoRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        turnoRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return turnoRepository.existsById(id);
    }

    // Relationship management stuff
    /**
     * Add a alunos to Turno
     * @param id the Turno id
     * @param alunos list of Aluno to add
     * @return the updated Turno
     */
    public Turno addAlunos(Long id, List<Aluno> alunos) {
        Turno turno = turnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turno not found: " + id));

        // Add all (so it works for batch operation)
        for (Aluno aluno : alunos) {
            turno.getAlunos().add(aluno);
        }

        // Validate parent entity after adding
        List<String> errors = turno.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turnoRepository.save(turno);
    }

    /**
     * Remove a Aluno from Turno
     * @param id the Turno id
     * @param alunoIds the ids of the Aluno to remove
     * @return the updated Turno
     */
    public Turno removeAlunos(Long id, List<Long> alunoIds) {
        Turno turno = turnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turno not found: " + id));

        // Remove all (batch operation)
        turno.getAlunos().removeIf(aluno -> alunoIds.contains(aluno.getId()));

        return turnoRepository.save(turno);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return boolean
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public boolean temVaga(Long id) {
        throw new UnsupportedOperationException(
            "Method 'temVaga' not yet implemented. "
        );
    }

}
