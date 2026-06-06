package com.example.app.service;

import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;
import com.example.app.dto.TurnoCreateDTO;
import com.example.app.dto.TurnoResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;

import com.example.app.models.EstadoAluno;
@Service
@Transactional
@RequiredArgsConstructor
public class TurnoService {

    // Dependencies
    private final TurnoRepository turnoRepository;
    private final AlunoRepository alunoRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<TurnoResponseDTO> findAll() {
        return turnoRepository.findAll().stream()
            .map(TurnoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<TurnoResponseDTO> findById(Long id) {
        return turnoRepository.findById(id)
            .map(TurnoResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public TurnoResponseDTO create(TurnoCreateDTO dto) {
        Turno turno = dto.toEntity();

        Turno saved = turnoRepository.save(turno);
        return TurnoResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public TurnoResponseDTO update(Long id, TurnoCreateDTO dto) {
        Turno existing = turnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Turno not found: " + id));

        // Update attributes from DTO
        existing.setNumero(dto.getNumero());
        existing.setCapacidade(dto.getCapacidade());

        Turno saved = turnoRepository.save(existing);
        return TurnoResponseDTO.fromEntity(saved);
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

    // operations (TODO: implement)

    public Boolean temVaga() {
        throw new UnsupportedOperationException(
            "Method 'temVaga' not yet implemented. "
        );
    }
}
