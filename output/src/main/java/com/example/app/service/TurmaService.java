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

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add Aluno entities to Turma
     * @param turmaId the Turma id
     * @param alunoIds list of Aluno ids to add
     * @return the updated Turma
     */
    public Turma addAlunos(Long turmaId, List<Long> alunoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));
        List<Aluno> alunos = alunoRepository.findAllById(alunoIds);

        for (Aluno aluno : alunos) {
            turma.getAlunos().put(aluno.getNumero(), aluno);
        }

        return turmaRepository.save(turma);
    }

    /**
     * OneToMany (unidirectional): Remove Aluno entities from Turma
     * @param turmaId the Turma id
     * @param alunoIds list of Aluno ids to remove
     * @return the updated Turma
     */
    public Turma removeAlunos(Long turmaId, List<Long> alunoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));

        List<Aluno> alunos = alunoRepository.findAllById(alunoIds);
        for (Aluno aluno : alunos) {
            turma.getAlunos().remove(aluno.getNumero());
        }

        return turmaRepository.save(turma);
    }


    /**
     * OneToMany (unidirectional): Add Turno entities to Turma
     * @param turmaId the Turma id
     * @param turnoIds list of Turno ids to add
     * @return the updated Turma
     */
    public Turma addTurnos(Long turmaId, List<Long> turnoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));
        List<Turno> turnos = turnoRepository.findAllById(turnoIds);

        turma.getTurnos().addAll(turnos);

        return turmaRepository.save(turma);
    }

    /**
     * OneToMany (unidirectional): Remove Turno entities from Turma
     * @param turmaId the Turma id
     * @param turnoIds list of Turno ids to remove
     * @return the updated Turma
     */
    public Turma removeTurnos(Long turmaId, List<Long> turnoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));

        turma.getTurnos().removeIf(turno -> turnoIds.contains(turno.getId()));

        return turmaRepository.save(turma);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return int
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public int totalAlunos(Long id) {
        throw new UnsupportedOperationException(
            "Method 'totalAlunos' not yet implemented. "
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
     * @param alunos List<Aluno>
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public void inicializarAlunosParaTeste(Long id, List<Aluno> alunos) {
        throw new UnsupportedOperationException(
            "Method 'inicializarAlunosParaTeste' not yet implemented. "
        );
    }

}
