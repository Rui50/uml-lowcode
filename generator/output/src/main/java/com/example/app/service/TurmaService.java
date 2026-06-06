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
        existing.setNumAlunos(dto.getNumAlunos());
        existing.setNome(dto.getNome());

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

        Turma saved = turmaRepository.save(turma);
        return TurmaResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public int contarAlunos() {
        throw new UnsupportedOperationException(
            "Method 'contarAlunos' not yet implemented. "
        );
    }

    public void inicializarAlunosParaTeste(List<Aluno> alunos) {
        throw new UnsupportedOperationException(
            "Method 'inicializarAlunosParaTeste' not yet implemented. "
        );
    }

    public void inscrever(Aluno a) {
        throw new UnsupportedOperationException(
            "Method 'inscrever' not yet implemented. "
        );
    }

    public boolean transferirAluno(Long turmaId, int numero, Long destinoId) {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        if (!(turma.getAlunos().size() > 0)) {
            throw new IllegalStateException("a turma tem de ter alunos");
        }
        Map<Integer, Aluno> alunos_pre = new HashMap<>(turma.getAlunos());
        Turma destino = turmaRepository.findById(destinoId)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        boolean sucesso = false;
        Aluno a = turma.getAlunos().get(numero);
        if (a != null) {
            turma.getAlunos().remove(a.getNumero());
            destino.getAlunos().put(a.getNumero(), a);
            sucesso = true;
        }
        if (!(java.util.Objects.equals(turma.getAlunos().size(), alunos_pre.size() - 1))) {
            throw new IllegalStateException("tem de diminuir 1");
        }
        return sucesso;
    }

    public void fecharTurma() {
        throw new UnsupportedOperationException(
            "Method 'fecharTurma' not yet implemented. "
        );
    }
}
