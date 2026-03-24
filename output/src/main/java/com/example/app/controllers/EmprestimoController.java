package com.example.app.controllers;

import com.example.app.models.Emprestimo;
import com.example.app.service.EmprestimoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Emprestimo
 */
@RestController
@RequestMapping("/api/emprestimos")
@RequiredArgsConstructor
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    // CRUD Endpoints

    /**
     * GET /api/emprestimos
     * Retrieve all Emprestimo entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Emprestimo>> getAll() {
        List<Emprestimo> entities = emprestimoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/emprestimos/{id}
     * Retrieve Emprestimo by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Emprestimo> getById(@PathVariable Long id) {
        return emprestimoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/emprestimos
     * Create new Emprestimo
     * @param emprestimo entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Emprestimo> create(@RequestBody Emprestimo emprestimo) {
        Emprestimo created = emprestimoService.create(emprestimo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Emprestimo> update(
            @PathVariable Long id,
            @RequestBody Emprestimo emprestimo) {
        Emprestimo updated = emprestimoService.update(id, emprestimo);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/emprestimos/{id}
     * Delete Emprestimo
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!emprestimoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        emprestimoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
