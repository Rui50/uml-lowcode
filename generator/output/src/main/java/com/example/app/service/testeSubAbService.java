package com.example.app.service;

import com.example.app.models.testeSubAb;
import com.example.app.repository.testeSubAbRepository;
import com.example.app.dto.testeSubAbCreateDTO;
import com.example.app.dto.testeSubAbResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.EstadoAluno;
@Service
@Transactional
@RequiredArgsConstructor
public class testeSubAbService {

    // Dependencies
    private final testeSubAbRepository testeSubAbRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<testeSubAbResponseDTO> findAll() {
        return testeSubAbRepository.findAll().stream()
            .map(testeSubAbResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<testeSubAbResponseDTO> findById(Long id) {
        return testeSubAbRepository.findById(id)
            .map(testeSubAbResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public testeSubAbResponseDTO create(testeSubAbCreateDTO dto) {
        testeSubAb testeSubAb = dto.toEntity();

        testeSubAb saved = testeSubAbRepository.save(testeSubAb);
        return testeSubAbResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public testeSubAbResponseDTO update(Long id, testeSubAbCreateDTO dto) {
        testeSubAb existing = testeSubAbRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("testeSubAb not found: " + id));

        // Update attributes from DTO
        existing.setNada(dto.getNada());
        existing.setTestar(dto.getTestar());

        testeSubAb saved = testeSubAbRepository.save(existing);
        return testeSubAbResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        testeSubAbRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return testeSubAbRepository.existsById(id);
    }

    // operations (TODO: implement)

    public void testeOp() {
        throw new UnsupportedOperationException(
            "Method 'testeOp' not yet implemented. "
        );
    }
}
