package com.example.app.controllers;

import com.example.app.models.Leitor;
import com.example.app.service.LeitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Emprestimo;

/**
 * REST Controller for Leitor
 */
@RestController
@RequestMapping("/api/leitors")
@RequiredArgsConstructor
public class LeitorController {

    private final LeitorService leitorService;

    // CRUD Endpoints

    /**
     * GET /api/leitors
     * Retrieve all Leitor entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Leitor>> getAll() {
        List<Leitor> entities = leitorService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/leitors/{id}
     * Retrieve Leitor by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Leitor> getById(@PathVariable Long id) {
        return leitorService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/leitors
     * Create new Leitor
     * @param leitor entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Leitor> create(@RequestBody Leitor leitor) {
        Leitor created = leitorService.create(leitor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Leitor> update(
            @PathVariable Long id,
            @RequestBody Leitor leitor) {
        Leitor updated = leitorService.update(id, leitor);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/leitors/{id}
     * Delete Leitor
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!leitorService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        leitorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Relationship Endpoints
    // ========================================================================

    /**
     * Add Emprestimo entities to Leitor by IDs
     */
    @PostMapping("/{id}/emprestimos")
    public ResponseEntity<Leitor> addEmprestimos(
            @PathVariable Long id,
            @RequestBody List<Long> emprestimoIds) {
        Leitor updated = leitorService.addEmprestimos(id, emprestimoIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Emprestimo entities from Leitor by IDs
     */
    @DeleteMapping("/{id}/emprestimos")
    public ResponseEntity<Leitor> removeEmprestimos(
            @PathVariable Long id,
            @RequestBody List<Long> emprestimoIds) {
        Leitor updated = leitorService.removeEmprestimos(id, emprestimoIds);
        return ResponseEntity.ok(updated);
    }

}
