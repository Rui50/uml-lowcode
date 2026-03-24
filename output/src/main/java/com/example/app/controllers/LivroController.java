package com.example.app.controllers;

import com.example.app.models.Livro;
import com.example.app.service.LivroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Exemplar;
import com.example.app.models.Autor;

/**
 * REST Controller for Livro
 */
@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
public class LivroController {

    private final LivroService livroService;

    // CRUD Endpoints

    /**
     * GET /api/livros
     * Retrieve all Livro entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Livro>> getAll() {
        List<Livro> entities = livroService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/livros/{id}
     * Retrieve Livro by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Livro> getById(@PathVariable Long id) {
        return livroService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/livros
     * Create new Livro
     * @param livro entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Livro> create(@RequestBody Livro livro) {
        Livro created = livroService.create(livro);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Livro> update(
            @PathVariable Long id,
            @RequestBody Livro livro) {
        Livro updated = livroService.update(id, livro);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/livros/{id}
     * Delete Livro
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!livroService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        livroService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Relationship Endpoints
    // ========================================================================

    /**
     * Add Exemplar entities to Livro by IDs
     */
    @PostMapping("/{id}/exemplars")
    public ResponseEntity<Livro> addExemplars(
            @PathVariable Long id,
            @RequestBody List<Long> exemplarIds) {
        Livro updated = livroService.addExemplars(id, exemplarIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Exemplar entities from Livro by IDs
     */
    @DeleteMapping("/{id}/exemplars")
    public ResponseEntity<Livro> removeExemplars(
            @PathVariable Long id,
            @RequestBody List<Long> exemplarIds) {
        Livro updated = livroService.removeExemplars(id, exemplarIds);
        return ResponseEntity.ok(updated);
    }

}
