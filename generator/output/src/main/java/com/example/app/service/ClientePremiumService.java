package com.example.app.service;

import com.example.app.models.ClientePremium;
import com.example.app.repository.ClientePremiumRepository;
import com.example.app.dto.ClientePremiumCreateDTO;
import com.example.app.dto.ClientePremiumResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ClientePremiumService {

    // Dependencies
    private final ClientePremiumRepository clientePremiumRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<ClientePremiumResponseDTO> findAll() {
        return clientePremiumRepository.findAll().stream()
            .map(ClientePremiumResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<ClientePremiumResponseDTO> findById(Long id) {
        return clientePremiumRepository.findById(id)
            .map(ClientePremiumResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public ClientePremiumResponseDTO create(ClientePremiumCreateDTO dto) {
        ClientePremium clientePremium = dto.toEntity();

        ClientePremium saved = clientePremiumRepository.save(clientePremium);

        return ClientePremiumResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public ClientePremiumResponseDTO update(Long id, ClientePremiumCreateDTO dto) {
        ClientePremium existing = clientePremiumRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("ClientePremium not found: " + id));

        // Update attributes from DTO
        existing.setDesconto(dto.getDesconto());
        existing.setPontos(dto.getPontos());
        existing.setNome(dto.getNome());
        existing.setEmail(dto.getEmail());

        ClientePremium saved = clientePremiumRepository.saveAndFlush(existing);
        return ClientePremiumResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        clientePremiumRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return clientePremiumRepository.existsById(id);
    }

}
