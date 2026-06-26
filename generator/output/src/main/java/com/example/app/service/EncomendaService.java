package com.example.app.service;

import com.example.app.models.Encomenda;
import com.example.app.repository.EncomendaRepository;
import com.example.app.dto.EncomendaCreateDTO;
import com.example.app.dto.EncomendaResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.LinhaEncomenda;
import com.example.app.repository.LinhaEncomendaRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class EncomendaService {

    // Dependencies
    private final EncomendaRepository encomendaRepository;
    private final LinhaEncomendaRepository linhaEncomendaRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<EncomendaResponseDTO> findAll() {
        return encomendaRepository.findAll().stream()
            .map(EncomendaResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<EncomendaResponseDTO> findById(Long id) {
        return encomendaRepository.findById(id)
            .map(EncomendaResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public EncomendaResponseDTO create(EncomendaCreateDTO dto) {
        Encomenda encomenda = dto.toEntity();

        Encomenda saved = encomendaRepository.save(encomenda);

        if (dto.getLinhasIds() != null) {
            for (LinhaEncomenda linhas : linhaEncomendaRepository.findAllById(dto.getLinhasIds())) {
                saved.getLinhas().add(linhas);
            }
        }
        saved = encomendaRepository.save(saved);
        return EncomendaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public EncomendaResponseDTO update(Long id, EncomendaCreateDTO dto) {
        Encomenda existing = encomendaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Encomenda not found: " + id));

        // Update attributes from DTO
        existing.setData(dto.getData());
        existing.setEstado(dto.getEstado());

        if (dto.getLinhasIds() != null) {
            for (LinhaEncomenda linhas : linhaEncomendaRepository.findAllById(dto.getLinhasIds())) {
                existing.getLinhas().add(linhas);
            }
        }
        Encomenda saved = encomendaRepository.saveAndFlush(existing);
        return EncomendaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        encomendaRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return encomendaRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add LinhaEncomenda entities to Encomenda
     * @param encomendaId the Encomenda id
     * @param linhaEncomendaIds list of LinhaEncomenda ids to add
     * @return the updated Encomenda
     */
    public EncomendaResponseDTO addLinhas(Long encomendaId, List<Long> linhaEncomendaIds) {
        Encomenda encomenda = encomendaRepository.findById(encomendaId)
            .orElseThrow(() -> new RuntimeException("Encomenda not found: " + encomendaId));
        List<LinhaEncomenda> linhaEncomendas = linhaEncomendaRepository.findAllById(linhaEncomendaIds);

        encomenda.getLinhas().addAll(linhaEncomendas);

        Encomenda saved = encomendaRepository.save(encomenda);
        return EncomendaResponseDTO.fromEntity(saved);
    }

    /**
     * OneToMany (unidirectional): Remove LinhaEncomenda entities from Encomenda
     * @param encomendaId the Encomenda id
     * @param linhaEncomendaIds list of LinhaEncomenda ids to remove
     * @return the updated Encomenda
     */
    public EncomendaResponseDTO removeLinhas(Long encomendaId, List<Long> linhaEncomendaIds) {
        Encomenda encomenda = encomendaRepository.findById(encomendaId)
            .orElseThrow(() -> new RuntimeException("Encomenda not found: " + encomendaId));

        encomenda.getLinhas().removeIf(linhaEncomenda -> linhaEncomendaIds.contains(linhaEncomenda.getId()));

        Encomenda saved = encomendaRepository.save(encomenda);
        return EncomendaResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public double calcularTotal() {
        throw new UnsupportedOperationException(
            "Method 'calcularTotal' not yet implemented. "
        );
    }
}
