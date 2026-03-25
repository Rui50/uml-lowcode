package com.example.app.controllers;

import com.example.app.models.Aluno;
import com.example.app.service.AlunoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Turno;

/**
 * REST Controller for Aluno
 */
@RestController
@RequestMapping("/api/alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService alunoService;

    // CRUD Endpoints

    /**
     * GET /api/alunos
     * Retrieve all Aluno entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Aluno>> getAll() {
        List<Aluno> entities = alunoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/alunos/{id}
     * Retrieve Aluno by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Aluno> getById(@PathVariable Long id) {
        return alunoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/alunos
     * Create new Aluno
     * @param aluno entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<Aluno> create(@RequestBody Aluno aluno) {
        Aluno created = alunoService.create(aluno);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Aluno> update(
            @PathVariable Long id,
            @RequestBody Aluno aluno) {
        Aluno updated = alunoService.update(id, aluno);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/alunos/{id}
     * Delete Aluno
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!alunoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        alunoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Relationship Endpoints
    // ========================================================================

    /**
     * ManyToOne: Assign a Turno to Aluno
     */
    @PutMapping("/{id}/turno/{turnoId}")
    public ResponseEntity<Aluno> assignTurno(
            @PathVariable Long id,
            @PathVariable Long turnoId) {
        Aluno updated = alunoService.assignTurno(id, turnoId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Turno from Aluno
     */
    @DeleteMapping("/{id}/turno")
    public ResponseEntity<Aluno> removeTurno(
            @PathVariable Long id) {
        Aluno updated = alunoService.removeTurno(id);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia

    @PostMapping("/{id}/validar")
    public ResponseEntity<?> validar(
            @PathVariable Long id) {

        List<String> result = alunoService.validar(id);
        return ResponseEntity.ok(result);
    }
}
