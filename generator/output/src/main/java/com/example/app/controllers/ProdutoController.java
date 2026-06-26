package com.example.app.controllers;

import com.example.app.models.Produto;
import com.example.app.service.ProdutoService;
import com.example.app.dto.ProdutoCreateDTO;
import com.example.app.dto.ProdutoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Produto
 */
@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    // CRUD Endpoints

    /**
     * GET /api/produtos
     * Retrieve all Produto entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> getAll() {
        List<ProdutoResponseDTO> entities = produtoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/produtos/{id}
     * Retrieve Produto by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> getById(@PathVariable Long id) {
        return produtoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/produtos
     * Create new Produto
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> create(@RequestBody ProdutoCreateDTO dto) {
        ProdutoResponseDTO created = produtoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ProdutoCreateDTO dto) {
        ProdutoResponseDTO updated = produtoService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/produtos/{id}
     * Delete Produto
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!produtoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        produtoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
