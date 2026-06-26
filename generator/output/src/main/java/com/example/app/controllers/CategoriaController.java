package com.example.app.controllers;

import com.example.app.models.Categoria;
import com.example.app.service.CategoriaService;
import com.example.app.dto.CategoriaCreateDTO;
import com.example.app.dto.CategoriaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.Produto;

/**
 * REST Controller for Categoria
 */
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    // CRUD Endpoints

    /**
     * GET /api/categorias
     * Retrieve all Categoria entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> getAll() {
        List<CategoriaResponseDTO> entities = categoriaService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/categorias/{id}
     * Retrieve Categoria by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> getById(@PathVariable Long id) {
        return categoriaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/categorias
     * Create new Categoria
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> create(@RequestBody CategoriaCreateDTO dto) {
        CategoriaResponseDTO created = categoriaService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> update(
            @PathVariable Long id,
            @RequestBody CategoriaCreateDTO dto) {
        CategoriaResponseDTO updated = categoriaService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/categorias/{id}
     * Delete Categoria
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!categoriaService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        categoriaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * ManyToOne: Assign a Produto to Categoria
     */
    @PutMapping("/{id}/produto/{produtoId}")
    public ResponseEntity<CategoriaResponseDTO> assignProduto(
            @PathVariable Long id,
            @PathVariable Long produtoId) {
        CategoriaResponseDTO updated = categoriaService.assignProduto(id, produtoId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Produto from Categoria
     */
    @DeleteMapping("/{id}/produto")
    public ResponseEntity<CategoriaResponseDTO> removeProduto(
            @PathVariable Long id) {
        CategoriaResponseDTO updated = categoriaService.removeProduto(id);
        return ResponseEntity.ok(updated);
    }

}
