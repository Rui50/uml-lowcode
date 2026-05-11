package com.example.app.service;

import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;
import com.example.app.dto.AlunoCreateDTO;
import com.example.app.dto.AlunoResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;

import com.example.app.models.EstadoAluno;
@Service
@Transactional
@RequiredArgsConstructor
public class AlunoService {

    // Dependencies
    private final AlunoRepository alunoRepository;
    private final TurnoRepository turnoRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<AlunoResponseDTO> findAll() {
        return alunoRepository.findAll().stream()
            .map(AlunoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<AlunoResponseDTO> findById(Long id) {
        return alunoRepository.findById(id)
            .map(AlunoResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public AlunoResponseDTO create(AlunoCreateDTO dto) {
        Aluno aluno = dto.toEntity();

        Aluno saved = alunoRepository.save(aluno);
        return AlunoResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public AlunoResponseDTO update(Long id, AlunoCreateDTO dto) {
        Aluno existing = alunoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Aluno not found: " + id));

        // Update attributes from DTO
        existing.setEmail(dto.getEmail());
        existing.setEstado(dto.getEstado());
        existing.setNome(dto.getNome());
        existing.setNumero(dto.getNumero());

        Aluno saved = alunoRepository.save(existing);
        return AlunoResponseDTO.fromEntity(saved);
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
    public AlunoResponseDTO assignTurno(Long alunoId, Long turnoId) {
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

        Aluno saved = alunoRepository.save(aluno);
        return AlunoResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Turno from Aluno
     * @param alunoId the Aluno id
     * @return the updated Aluno
     */
    public AlunoResponseDTO removeTurno(Long alunoId) {
        Aluno aluno = alunoRepository.findById(alunoId)
            .orElseThrow(() -> new RuntimeException("Aluno not found: " + alunoId));

        // Remove from both sides
        if (aluno.getTurno() != null) {
            aluno.getTurno().getAlunos().remove(aluno);
        }
        aluno.setTurno(null);

        Aluno saved = alunoRepository.save(aluno);
        return AlunoResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public List<String> validar() {
        throw new UnsupportedOperationException(
            "Method 'validar' not yet implemented. "
        );
    }
}
