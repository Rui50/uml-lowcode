package com.example.app.controllers;

import com.example.app.models.Encomenda;
import com.example.app.service.EncomendaService;
import com.example.app.dto.EncomendaCreateDTO;
import com.example.app.dto.EncomendaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.LinhaEncomenda;

/**
 * REST Controller for Encomenda
 */
@RestController
@RequestMapping("/api/encomendas")
@RequiredArgsConstructor
public class EncomendaController {

    private final EncomendaService encomendaService;

    // CRUD Endpoints

    /**
     * GET /api/encomendas
     * Retrieve all Encomenda entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<EncomendaResponseDTO>> getAll() {
        List<EncomendaResponseDTO> entities = encomendaService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/encomendas/{id}
     * Retrieve Encomenda by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<EncomendaResponseDTO> getById(@PathVariable Long id) {
        return encomendaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/encomendas
     * Create new Encomenda
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<EncomendaResponseDTO> create(@RequestBody EncomendaCreateDTO dto) {
        EncomendaResponseDTO created = encomendaService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EncomendaResponseDTO> update(
            @PathVariable Long id,
            @RequestBody EncomendaCreateDTO dto) {
        EncomendaResponseDTO updated = encomendaService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/encomendas/{id}
     * Delete Encomenda
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!encomendaService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        encomendaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * Add LinhaEncomenda entities to Encomenda by IDs
     */
    @PostMapping("/{id}/linhas")
    public ResponseEntity<EncomendaResponseDTO> addLinhas(
            @PathVariable Long id,
            @RequestBody List<Long> linhaEncomendaIds) {
        EncomendaResponseDTO updated = encomendaService.addLinhas(id, linhaEncomendaIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove LinhaEncomenda entities from Encomenda by IDs
     */
    @DeleteMapping("/{id}/linhas")
    public ResponseEntity<EncomendaResponseDTO> removeLinhas(
            @PathVariable Long id,
            @RequestBody List<Long> linhaEncomendaIds) {
        EncomendaResponseDTO updated = encomendaService.removeLinhas(id, linhaEncomendaIds);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
