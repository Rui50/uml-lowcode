package com.example.app.service;

import com.example.app.models.Turno;
import com.example.app.repository.AlunoRepository;
import com.example.app.repository.TurnoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Aluno;
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
     * @param alunoIds list of Aluno ids to add
     * @return the updated Turno
     */
    public Turno addAlunos(Long id, List<Long> alunoIds) {
        Turno turno = findTurnoOrThrow(id);
        List<Aluno> alunos = findAlunosOrThrow(alunoIds);

        for (Aluno aluno : alunos) {
            assignAluno(turno, aluno);
        }

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
        Turno turno = findTurnoOrThrow(id);
        List<Aluno> alunos = findAlunosOrThrow(alunoIds);

        for (Aluno aluno : alunos) {
            if (isAssignedToTurno(turno, aluno)) {
                turno.getAlunos().removeIf(existing ->
                    Objects.equals(existing.getId(), aluno.getId())
                );
                aluno.setTurno(null);
            }
        }

        return turnoRepository.save(turno);
    }

    private Turno findTurnoOrThrow(Long id) {
        return turnoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Turno not found: " + id));
    }

    private List<Aluno> findAlunosOrThrow(List<Long> alunoIds) {
        if (alunoIds == null) {
            throw new IllegalArgumentException("Aluno IDs are required.");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(alunoIds);
        if (uniqueIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Aluno> alunos = alunoRepository.findAllById(uniqueIds);
        Set<Long> foundIds = new HashSet<>();
        for (Aluno aluno : alunos) {
            foundIds.add(aluno.getId());
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long alunoId : uniqueIds) {
            if (!foundIds.contains(alunoId)) {
                missingIds.add(alunoId);
            }
        }

        if (!missingIds.isEmpty()) {
            throw new NoSuchElementException("Aluno not found: " + missingIds);
        }

        Map<Long, Integer> positions = new HashMap<>();
        int index = 0;
        for (Long alunoId : uniqueIds) {
            positions.put(alunoId, index++);
        }

        alunos.sort(Comparator.comparingInt(aluno -> positions.get(aluno.getId())));
        return alunos;
    }

    private void assignAluno(Turno turno, Aluno aluno) {
        if (isAssignedToTurno(turno, aluno)) {
            ensureAlunoInTurnoCollection(turno, aluno);
            return;
        }

        Turno previousTurno = aluno.getTurno();
        if (previousTurno != null) {
            previousTurno.getAlunos().removeIf(existing ->
                Objects.equals(existing.getId(), aluno.getId())
            );
        }

        aluno.setTurno(turno);
        ensureAlunoInTurnoCollection(turno, aluno);
    }

    private boolean isAssignedToTurno(Turno turno, Aluno aluno) {
        return aluno.getTurno() != null
            && Objects.equals(aluno.getTurno().getId(), turno.getId());
    }

    private void ensureAlunoInTurnoCollection(Turno turno, Aluno aluno) {
        boolean alreadyPresent = turno.getAlunos().stream()
            .anyMatch(existing -> Objects.equals(existing.getId(), aluno.getId()));
        if (!alreadyPresent) {
            turno.getAlunos().add(aluno);
        }
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
