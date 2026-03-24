package com.example.app.controllers;

import com.example.app.models.Exemplar;
import com.example.app.service.ExemplarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Emprestimo;

/**
 * REST Controller for Exemplar
 */
@RestController
@RequestMapping("/api/exemplars")
@RequiredArgsConstructor
public class ExemplarController {

    private final ExemplarService exemplarService;

    // CRUD Endpoints

    /**
     * GET /api/exemplars
     * Retrieve all Exemplar entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Exemplar>> getAll() {
        List<Exemplar> entities = exemplarService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/exemplars/{id}
     * Retrieve Exemplar by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Exemplar> getById(@PathVariable Long id) {
        return exemplarService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/exemplars
     * Create new Exemplar
     * @param exemplar entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Exemplar> create(@RequestBody Exemplar exemplar) {
        Exemplar created = exemplarService.create(exemplar);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Exemplar> update(
            @PathVariable Long id,
            @RequestBody Exemplar exemplar) {
        Exemplar updated = exemplarService.update(id, exemplar);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/exemplars/{id}
     * Delete Exemplar
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!exemplarService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        exemplarService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Relationship Endpoints
    // ========================================================================

    /**
     * Add Emprestimo entities to Exemplar by IDs
     */
    @PostMapping("/{id}/emprestimos")
    public ResponseEntity<Exemplar> addEmprestimos(
            @PathVariable Long id,
            @RequestBody List<Long> emprestimoIds) {
        Exemplar updated = exemplarService.addEmprestimos(id, emprestimoIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Emprestimo entities from Exemplar by IDs
     */
    @DeleteMapping("/{id}/emprestimos")
    public ResponseEntity<Exemplar> removeEmprestimos(
            @PathVariable Long id,
            @RequestBody List<Long> emprestimoIds) {
        Exemplar updated = exemplarService.removeEmprestimos(id, emprestimoIds);
        return ResponseEntity.ok(updated);
    }

}
