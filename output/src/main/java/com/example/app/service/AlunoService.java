package com.example.app.service;

import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class AlunoService {

    // Dependencies
    private final AlunoRepository alunoRepository;
    private final TurnoRepository turnoRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Aluno> findAll() {
        return alunoRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Aluno> findById(Long id) {
        return alunoRepository.findById(id);
    }

    /**
     * @param aluno the entity to create
     * @return the created entity
     */
    public Aluno create(Aluno aluno) {
        return alunoRepository.save(aluno);
    }

    /**
     * @param id the entity id
     * @param aluno the entity data to update
     * @return the updated entity
     */
    public Aluno update(Long id, Aluno aluno) {
        // Find existing entity
        Aluno existing = alunoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Aluno not found: " + id));

        // Update all attributes
        existing.setEmail(aluno.getEmail());
        existing.setNome(aluno.getNome());
        existing.setNumero(aluno.getNumero());


        // para ja coleções nao sao updated aqui

        return alunoRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        alunoRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return alunoRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToOne: Assign a Turno to Aluno
     * @param alunoId the Aluno id
     * @param turnoId the Turno id
     * @return the updated Aluno
     */
    public Aluno assignTurno(Long alunoId, Long turnoId) {
        Aluno aluno = alunoRepository.findById(alunoId)
            .orElseThrow(() -> new RuntimeException("Aluno not found: " + alunoId));
        Turno turno = turnoRepository.findById(turnoId)
            .orElseThrow(() -> new RuntimeException("Turno not found: " + turnoId));

        // Remove from previous Turno if exists
        if (aluno.getTurno() != null) {
            aluno.getTurno().getAlunos().remove(aluno);
        }

        // Set both sides of the relationship
        aluno.setTurno(turno);
        turno.getAlunos().add(aluno);

        return alunoRepository.save(aluno);
    }

    /**
     * ManyToOne: Remove the Turno from Aluno
     * @param alunoId the Aluno id
     * @return the updated Aluno
     */
    public Aluno removeTurno(Long alunoId) {
        Aluno aluno = alunoRepository.findById(alunoId)
            .orElseThrow(() -> new RuntimeException("Aluno not found: " + alunoId));

        // Remove from both sides
        if (aluno.getTurno() != null) {
            aluno.getTurno().getAlunos().remove(aluno);
        }
        aluno.setTurno(null);

        return alunoRepository.save(aluno);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return List<String>
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public List<String> validar(Long id) {
        throw new UnsupportedOperationException(
            "Method 'validar' not yet implemented. "
        );
    }

}
