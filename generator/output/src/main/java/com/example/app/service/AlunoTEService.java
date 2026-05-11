package com.example.app.service;

import com.example.app.models.AlunoTE;
import com.example.app.repository.AlunoTERepository;
import com.example.app.dto.AlunoTECreateDTO;
import com.example.app.dto.AlunoTEResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.EstadoAluno;
@Service
@Transactional
@RequiredArgsConstructor
public class AlunoTEService {

    // Dependencies
    private final AlunoTERepository alunoTERepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<AlunoTEResponseDTO> findAll() {
        return alunoTERepository.findAll().stream()
            .map(AlunoTEResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<AlunoTEResponseDTO> findById(Long id) {
        return alunoTERepository.findById(id)
            .map(AlunoTEResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public AlunoTEResponseDTO create(AlunoTECreateDTO dto) {
        AlunoTE alunoTE = dto.toEntity();

        AlunoTE saved = alunoTERepository.save(alunoTE);
        return AlunoTEResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public AlunoTEResponseDTO update(Long id, AlunoTECreateDTO dto) {
        AlunoTE existing = alunoTERepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AlunoTE not found: " + id));

        // Update attributes from DTO
        existing.setEmpresa(dto.getEmpresa());
        existing.setEmail(dto.getEmail());
        existing.setEstado(dto.getEstado());
        existing.setNome(dto.getNome());
        existing.setNumero(dto.getNumero());

        AlunoTE saved = alunoTERepository.save(existing);
        return AlunoTEResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        alunoTERepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return alunoTERepository.existsById(id);
    }

    // operations (TODO: implement)

    public Boolean comprovativo() {
        throw new UnsupportedOperationException(
            "Method 'comprovativo' not yet implemented. "
        );
    }
}
