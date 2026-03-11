package com.example.app.controller;

import com.example.app.models.Aluno;
import com.example.app.service.AlunoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Aluno
 */
@RestController
@RequestMapping("/api/Alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService alunoService;

    // ========================================================================
    // CRUD Endpoints
    // Generated from Class Diagram - Fully functional
    // ========================================================================

    /**
     * GET /api/Alunos
     * Retrieve all Aluno entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Aluno>> getAll() {
        List<Aluno> entities = alunoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/Alunos/{id}
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
     * POST /api/Alunos
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
     * DELETE /api/Alunos/{id}
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

}
