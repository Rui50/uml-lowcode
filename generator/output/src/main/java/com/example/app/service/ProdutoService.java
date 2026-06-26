package com.example.app.service;

import com.example.app.models.Produto;
import com.example.app.repository.ProdutoRepository;
import com.example.app.dto.ProdutoCreateDTO;
import com.example.app.dto.ProdutoResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProdutoService {

    // Dependencies
    private final ProdutoRepository produtoRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<ProdutoResponseDTO> findAll() {
        return produtoRepository.findAll().stream()
            .map(ProdutoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<ProdutoResponseDTO> findById(Long id) {
        return produtoRepository.findById(id)
            .map(ProdutoResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public ProdutoResponseDTO create(ProdutoCreateDTO dto) {
        Produto produto = dto.toEntity();

        // Validate OCL invariants
        List<String> errors = produto.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Produto saved = produtoRepository.save(produto);

        return ProdutoResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public ProdutoResponseDTO update(Long id, ProdutoCreateDTO dto) {
        Produto existing = produtoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produto not found: " + id));

        // Update attributes from DTO
        existing.setNome(dto.getNome());
        existing.setPreco(dto.getPreco());
        existing.setStock(dto.getStock());

        // Validate OCL invariants
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        Produto saved = produtoRepository.saveAndFlush(existing);
        return ProdutoResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        produtoRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return produtoRepository.existsById(id);
    }

}
