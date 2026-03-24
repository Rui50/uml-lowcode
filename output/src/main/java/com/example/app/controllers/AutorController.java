package com.example.app.controllers;

import com.example.app.models.Autor;
import com.example.app.service.AutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Livro;

/**
 * REST Controller for Autor
 */
@RestController
@RequestMapping("/api/autors")
@RequiredArgsConstructor
public class AutorController {

    private final AutorService autorService;

    // CRUD Endpoints

    /**
     * GET /api/autors
     * Retrieve all Autor entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Autor>> getAll() {
        List<Autor> entities = autorService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/autors/{id}
     * Retrieve Autor by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Autor> getById(@PathVariable Long id) {
        return autorService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/autors
     * Create new Autor
     * @param autor entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Autor> create(@RequestBody Autor autor) {
        Autor created = autorService.create(autor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Autor> update(
            @PathVariable Long id,
            @RequestBody Autor autor) {
        Autor updated = autorService.update(id, autor);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/autors/{id}
     * Delete Autor
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!autorService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        autorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Relationship Endpoints
    // ========================================================================

    /**
     * Add Livro entities to Autor by IDs
     */
    @PostMapping("/{id}/livros")
    public ResponseEntity<Autor> addLivros(
            @PathVariable Long id,
            @RequestBody List<Long> livroIds) {
        Autor updated = autorService.addLivros(id, livroIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Livro entities from Autor by IDs
     */
    @DeleteMapping("/{id}/livros")
    public ResponseEntity<Autor> removeLivros(
            @PathVariable Long id,
            @RequestBody List<Long> livroIds) {
        Autor updated = autorService.removeLivros(id, livroIds);
        return ResponseEntity.ok(updated);
    }

}
