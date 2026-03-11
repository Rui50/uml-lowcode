package com.example.app.controller;

import com.example.app.models.Turma;
import com.example.app.service.TurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Aluno;

/**
 * REST Controller for Turma
 */
@RestController
@RequestMapping("/api/Turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final TurmaService turmaService;

    // ========================================================================
    // CRUD Endpoints
    // Generated from Class Diagram - Fully functional
    // ========================================================================

    /**
     * GET /api/Turmas
     * Retrieve all Turma entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Turma>> getAll() {
        List<Turma> entities = turmaService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/Turmas/{id}
     * Retrieve Turma by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Turma> getById(@PathVariable Long id) {
        return turmaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/Turmas
     * Create new Turma
     * @param turma entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Turma> create(@RequestBody Turma turma) {
        Turma created = turmaService.create(turma);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Turma> update(
            @PathVariable Long id,
            @RequestBody Turma turma) {
        Turma updated = turmaService.update(id, turma);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/Turmas/{id}
     * Delete Turma
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!turmaService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        turmaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
