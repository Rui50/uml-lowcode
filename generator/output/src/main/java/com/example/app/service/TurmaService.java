package com.example.app.service;

import com.example.app.models.Turma;
import com.example.app.repository.TurmaRepository;
import com.example.app.dto.TurmaCreateDTO;
import com.example.app.dto.TurmaResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;
import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;

import com.example.app.models.EstadoAluno;
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
     * @return list of all entities as DTOs
     */
    public List<TurmaResponseDTO> findAll() {
        return turmaRepository.findAll().stream()
            .map(TurmaResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<TurmaResponseDTO> findById(Long id) {
        return turmaRepository.findById(id)
            .map(TurmaResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public TurmaResponseDTO create(TurmaCreateDTO dto) {
        Turma turma = dto.toEntity();

        // Validate OCL invariants
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Turma saved = turmaRepository.save(turma);
        return TurmaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public TurmaResponseDTO update(Long id, TurmaCreateDTO dto) {
        Turma existing = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + id));

        // Update attributes from DTO
        existing.setNome(dto.getNome());

        // Validate OCL invariants
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Turma saved = turmaRepository.save(existing);
        return TurmaResponseDTO.fromEntity(saved);
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
    public TurmaResponseDTO addAlunos(Long turmaId, List<Long> alunoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));
        List<Aluno> alunos = alunoRepository.findAllById(alunoIds);

        for (Aluno aluno : alunos) {
            turma.getAlunos().put(aluno.getNumero(), aluno);
        }

        // Validate OCL invariants
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Turma saved = turmaRepository.save(turma);
        return TurmaResponseDTO.fromEntity(saved);
    }

    /**
     * OneToMany (unidirectional): Remove Aluno entities from Turma
     * @param turmaId the Turma id
     * @param alunoIds list of Aluno ids to remove
     * @return the updated Turma
     */
    public TurmaResponseDTO removeAlunos(Long turmaId, List<Long> alunoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));

        List<Aluno> alunos = alunoRepository.findAllById(alunoIds);
        for (Aluno aluno : alunos) {
            turma.getAlunos().remove(aluno.getNumero());
        }

        // Validate OCL invariants
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Turma saved = turmaRepository.save(turma);
        return TurmaResponseDTO.fromEntity(saved);
    }


    /**
     * OneToMany (unidirectional): Add Turno entities to Turma
     * @param turmaId the Turma id
     * @param turnoIds list of Turno ids to add
     * @return the updated Turma
     */
    public TurmaResponseDTO addTurnos(Long turmaId, List<Long> turnoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));
        List<Turno> turnos = turnoRepository.findAllById(turnoIds);

        turma.getTurnos().addAll(turnos);

        // Validate OCL invariants
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Turma saved = turmaRepository.save(turma);
        return TurmaResponseDTO.fromEntity(saved);
    }

    /**
     * OneToMany (unidirectional): Remove Turno entities from Turma
     * @param turmaId the Turma id
     * @param turnoIds list of Turno ids to remove
     * @return the updated Turma
     */
    public TurmaResponseDTO removeTurnos(Long turmaId, List<Long> turnoIds) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found: " + turmaId));

        turma.getTurnos().removeIf(turno -> turnoIds.contains(turno.getId()));

        // Validate OCL invariants
        List<String> errors = turma.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Turma saved = turmaRepository.save(turma);
        return TurmaResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public void transferirAluno(Long id, Long alunoId, Long destino) {
        Turma turma = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        Turma turmaDestino = turmaRepository.findById(destino)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        Aluno aluno = alunoRepository.findById(alunoId)
            .orElseThrow(() -> new RuntimeException("Aluno not found"));
        turma.getAlunos().remove(aluno.getNumero());
        turmaDestino.getAlunos().put(aluno.getNumero(), aluno);
        turmaRepository.save(turma);
        turmaRepository.save(turmaDestino);
    }

    public void fecharTurma(Long id) {
        Turma turma = turmaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        if (!(!turma.getAlunos().isEmpty())) {
            throw new IllegalStateException("Precondition 'naoVazia' violated.");
        }
        for (var aluno : turma.getAlunos().values()) {
            if (aluno.getEstado() == EstadoAluno.ATIVO) {
                aluno.setEstado(EstadoAluno.INATIVO);
            }
        }
        turmaRepository.save(turma);
    }

    public void inicializarAlunosParaTeste() {
        throw new UnsupportedOperationException(
            "Method 'inicializarAlunosParaTeste' not yet implemented. "
        );
    }

    public void totalAlunos() {
        throw new UnsupportedOperationException(
            "Method 'totalAlunos' not yet implemented. "
        );
    }
}
