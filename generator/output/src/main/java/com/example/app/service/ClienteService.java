package com.example.app.service;

import com.example.app.models.Cliente;
import com.example.app.repository.ClienteRepository;
import com.example.app.dto.ClienteCreateDTO;
import com.example.app.dto.ClienteResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Encomenda;
import com.example.app.repository.EncomendaRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService {

    // Dependencies
    private final ClienteRepository clienteRepository;
    private final EncomendaRepository encomendaRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<ClienteResponseDTO> findAll() {
        return clienteRepository.findAll().stream()
            .map(ClienteResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<ClienteResponseDTO> findById(Long id) {
        return clienteRepository.findById(id)
            .map(ClienteResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public ClienteResponseDTO create(ClienteCreateDTO dto) {
        Cliente cliente = dto.toEntity();

        Cliente saved = clienteRepository.save(cliente);

        if (dto.getEncomendasIds() != null) {
            for (Encomenda encomendas : encomendaRepository.findAllById(dto.getEncomendasIds())) {
                saved.getEncomendas().add(encomendas);
            }
        }
        saved = clienteRepository.save(saved);
        return ClienteResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public ClienteResponseDTO update(Long id, ClienteCreateDTO dto) {
        Cliente existing = clienteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente not found: " + id));

        // Update attributes from DTO
        existing.setNome(dto.getNome());
        existing.setEmail(dto.getEmail());

        if (dto.getEncomendasIds() != null) {
            for (Encomenda encomendas : encomendaRepository.findAllById(dto.getEncomendasIds())) {
                existing.getEncomendas().add(encomendas);
            }
        }
        Cliente saved = clienteRepository.saveAndFlush(existing);
        return ClienteResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        clienteRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return clienteRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add Encomenda entities to Cliente
     * @param clienteId the Cliente id
     * @param encomendaIds list of Encomenda ids to add
     * @return the updated Cliente
     */
    public ClienteResponseDTO addEncomendas(Long clienteId, List<Long> encomendaIds) {
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente not found: " + clienteId));
        List<Encomenda> encomendas = encomendaRepository.findAllById(encomendaIds);

        cliente.getEncomendas().addAll(encomendas);

        Cliente saved = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(saved);
    }

    /**
     * OneToMany (unidirectional): Remove Encomenda entities from Cliente
     * @param clienteId the Cliente id
     * @param encomendaIds list of Encomenda ids to remove
     * @return the updated Cliente
     */
    public ClienteResponseDTO removeEncomendas(Long clienteId, List<Long> encomendaIds) {
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente not found: " + clienteId));

        cliente.getEncomendas().removeIf(encomenda -> encomendaIds.contains(encomenda.getId()));

        Cliente saved = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(saved);
    }

}
