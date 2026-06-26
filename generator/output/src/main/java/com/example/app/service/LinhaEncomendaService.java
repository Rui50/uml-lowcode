package com.example.app.service;

import com.example.app.models.LinhaEncomenda;
import com.example.app.repository.LinhaEncomendaRepository;
import com.example.app.dto.LinhaEncomendaCreateDTO;
import com.example.app.dto.LinhaEncomendaResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Produto;
import com.example.app.repository.ProdutoRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class LinhaEncomendaService {

    // Dependencies
    private final LinhaEncomendaRepository linhaEncomendaRepository;
    private final ProdutoRepository produtoRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<LinhaEncomendaResponseDTO> findAll() {
        return linhaEncomendaRepository.findAll().stream()
            .map(LinhaEncomendaResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<LinhaEncomendaResponseDTO> findById(Long id) {
        return linhaEncomendaRepository.findById(id)
            .map(LinhaEncomendaResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public LinhaEncomendaResponseDTO create(LinhaEncomendaCreateDTO dto) {
        LinhaEncomenda linhaEncomenda = dto.toEntity();

        LinhaEncomenda saved = linhaEncomendaRepository.save(linhaEncomenda);

        if (dto.getProdutoId() != null) {
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto not found: " + dto.getProdutoId()));
            saved.setProduto(produto);
        } else {
            saved.setProduto(null);
        }
        saved = linhaEncomendaRepository.save(saved);
        return LinhaEncomendaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public LinhaEncomendaResponseDTO update(Long id, LinhaEncomendaCreateDTO dto) {
        LinhaEncomenda existing = linhaEncomendaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("LinhaEncomenda not found: " + id));

        // Update attributes from DTO
        existing.setQuantidade(dto.getQuantidade());
        existing.setPrecoUnitario(dto.getPrecoUnitario());

        if (dto.getProdutoId() != null) {
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto not found: " + dto.getProdutoId()));
            existing.setProduto(produto);
        } else {
            existing.setProduto(null);
        }
        LinhaEncomenda saved = linhaEncomendaRepository.saveAndFlush(existing);
        return LinhaEncomendaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        linhaEncomendaRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return linhaEncomendaRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToOne: Assign a Produto to LinhaEncomenda
     * @param linhaEncomendaId the LinhaEncomenda id
     * @param produtoId the Produto id
     * @return the updated LinhaEncomenda
     */
    public LinhaEncomendaResponseDTO assignProduto(Long linhaEncomendaId, Long produtoId) {
        LinhaEncomenda linhaEncomenda = linhaEncomendaRepository.findById(linhaEncomendaId)
            .orElseThrow(() -> new RuntimeException("LinhaEncomenda not found: " + linhaEncomendaId));
        Produto produto = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new RuntimeException("Produto not found: " + produtoId));

        linhaEncomenda.setProduto(produto);

        LinhaEncomenda saved = linhaEncomendaRepository.save(linhaEncomenda);
        return LinhaEncomendaResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Produto from LinhaEncomenda
     * @param linhaEncomendaId the LinhaEncomenda id
     * @return the updated LinhaEncomenda
     */
    public LinhaEncomendaResponseDTO removeProduto(Long linhaEncomendaId) {
        LinhaEncomenda linhaEncomenda = linhaEncomendaRepository.findById(linhaEncomendaId)
            .orElseThrow(() -> new RuntimeException("LinhaEncomenda not found: " + linhaEncomendaId));

        linhaEncomenda.setProduto(null);

        LinhaEncomenda saved = linhaEncomendaRepository.save(linhaEncomenda);
        return LinhaEncomendaResponseDTO.fromEntity(saved);
    }

}
