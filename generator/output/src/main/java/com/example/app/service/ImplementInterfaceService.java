package com.example.app.service;

import com.example.app.models.ImplementInterface;
import com.example.app.repository.ImplementInterfaceRepository;
import com.example.app.dto.ImplementInterfaceCreateDTO;
import com.example.app.dto.ImplementInterfaceResponseDTO;
import com.example.app.models.InterfaceTest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.EstadoAluno;
@Service
@Transactional
@RequiredArgsConstructor
public class ImplementInterfaceService implements InterfaceTest {

    // Dependencies
    private final ImplementInterfaceRepository implementInterfaceRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<ImplementInterfaceResponseDTO> findAll() {
        return implementInterfaceRepository.findAll().stream()
            .map(ImplementInterfaceResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<ImplementInterfaceResponseDTO> findById(Long id) {
        return implementInterfaceRepository.findById(id)
            .map(ImplementInterfaceResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public ImplementInterfaceResponseDTO create(ImplementInterfaceCreateDTO dto) {
        ImplementInterface implementInterface = dto.toEntity();

        ImplementInterface saved = implementInterfaceRepository.save(implementInterface);
        return ImplementInterfaceResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public ImplementInterfaceResponseDTO update(Long id, ImplementInterfaceCreateDTO dto) {
        ImplementInterface existing = implementInterfaceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("ImplementInterface not found: " + id));

        ImplementInterface saved = implementInterfaceRepository.save(existing);
        return ImplementInterfaceResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        implementInterfaceRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return implementInterfaceRepository.existsById(id);
    }

    // operations (TODO: implement)

    public void start() {
        throw new UnsupportedOperationException(
            "Method 'start' not yet implemented. "
        );
    }
}
