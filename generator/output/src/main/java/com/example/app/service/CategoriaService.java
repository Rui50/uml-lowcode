package com.example.app.service;

import com.example.app.models.Categoria;
import com.example.app.repository.CategoriaRepository;
import com.example.app.dto.CategoriaCreateDTO;
import com.example.app.dto.CategoriaResponseDTO;

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
public class CategoriaService {

    // Dependencies
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<CategoriaResponseDTO> findAll() {
        return categoriaRepository.findAll().stream()
            .map(CategoriaResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<CategoriaResponseDTO> findById(Long id) {
        return categoriaRepository.findById(id)
            .map(CategoriaResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public CategoriaResponseDTO create(CategoriaCreateDTO dto) {
        Categoria categoria = dto.toEntity();

        Categoria saved = categoriaRepository.save(categoria);

        if (dto.getProdutoId() != null) {
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto not found: " + dto.getProdutoId()));
            saved.setProduto(produto);
        } else {
            saved.setProduto(null);
        }
        saved = categoriaRepository.save(saved);
        return CategoriaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public CategoriaResponseDTO update(Long id, CategoriaCreateDTO dto) {
        Categoria existing = categoriaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoria not found: " + id));

        // Update attributes from DTO
        existing.setNome(dto.getNome());
        existing.setDescricao(dto.getDescricao());

        if (dto.getProdutoId() != null) {
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto not found: " + dto.getProdutoId()));
            existing.setProduto(produto);
        } else {
            existing.setProduto(null);
        }
        Categoria saved = categoriaRepository.saveAndFlush(existing);
        return CategoriaResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        categoriaRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return categoriaRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToOne: Assign a Produto to Categoria
     * @param categoriaId the Categoria id
     * @param produtoId the Produto id
     * @return the updated Categoria
     */
    public CategoriaResponseDTO assignProduto(Long categoriaId, Long produtoId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new RuntimeException("Categoria not found: " + categoriaId));
        Produto produto = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new RuntimeException("Produto not found: " + produtoId));

        categoria.setProduto(produto);

        Categoria saved = categoriaRepository.save(categoria);
        return CategoriaResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Produto from Categoria
     * @param categoriaId the Categoria id
     * @return the updated Categoria
     */
    public CategoriaResponseDTO removeProduto(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new RuntimeException("Categoria not found: " + categoriaId));

        categoria.setProduto(null);

        Categoria saved = categoriaRepository.save(categoria);
        return CategoriaResponseDTO.fromEntity(saved);
    }

}
