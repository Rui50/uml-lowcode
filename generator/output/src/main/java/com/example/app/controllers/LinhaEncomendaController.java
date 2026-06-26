package com.example.app.controllers;

import com.example.app.models.LinhaEncomenda;
import com.example.app.service.LinhaEncomendaService;
import com.example.app.dto.LinhaEncomendaCreateDTO;
import com.example.app.dto.LinhaEncomendaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.Produto;

/**
 * REST Controller for LinhaEncomenda
 */
@RestController
@RequestMapping("/api/linhaencomendas")
@RequiredArgsConstructor
public class LinhaEncomendaController {

    private final LinhaEncomendaService linhaEncomendaService;

    // CRUD Endpoints

    /**
     * GET /api/linhaencomendas
     * Retrieve all LinhaEncomenda entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<LinhaEncomendaResponseDTO>> getAll() {
        List<LinhaEncomendaResponseDTO> entities = linhaEncomendaService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/linhaencomendas/{id}
     * Retrieve LinhaEncomenda by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<LinhaEncomendaResponseDTO> getById(@PathVariable Long id) {
        return linhaEncomendaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/linhaencomendas
     * Create new LinhaEncomenda
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<LinhaEncomendaResponseDTO> create(@RequestBody LinhaEncomendaCreateDTO dto) {
        LinhaEncomendaResponseDTO created = linhaEncomendaService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LinhaEncomendaResponseDTO> update(
            @PathVariable Long id,
            @RequestBody LinhaEncomendaCreateDTO dto) {
        LinhaEncomendaResponseDTO updated = linhaEncomendaService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/linhaencomendas/{id}
     * Delete LinhaEncomenda
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!linhaEncomendaService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        linhaEncomendaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * ManyToOne: Assign a Produto to LinhaEncomenda
     */
    @PutMapping("/{id}/produto/{produtoId}")
    public ResponseEntity<LinhaEncomendaResponseDTO> assignProduto(
            @PathVariable Long id,
            @PathVariable Long produtoId) {
        LinhaEncomendaResponseDTO updated = linhaEncomendaService.assignProduto(id, produtoId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Produto from LinhaEncomenda
     */
    @DeleteMapping("/{id}/produto")
    public ResponseEntity<LinhaEncomendaResponseDTO> removeProduto(
            @PathVariable Long id) {
        LinhaEncomendaResponseDTO updated = linhaEncomendaService.removeProduto(id);
        return ResponseEntity.ok(updated);
    }

}
