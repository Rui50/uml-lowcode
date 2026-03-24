package com.example.app.controllers;

import com.example.app.models.AlunoTE;
import com.example.app.service.AlunoTEService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for AlunoTE
 */
@RestController
@RequestMapping("/api/alunotes")
@RequiredArgsConstructor
public class AlunoTEController {

    private final AlunoTEService alunoTEService;

    // CRUD Endpoints

    /**
     * GET /api/alunotes
     * Retrieve all AlunoTE entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<AlunoTE>> getAll() {
        List<AlunoTE> entities = alunoTEService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/alunotes/{id}
     * Retrieve AlunoTE by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlunoTE> getById(@PathVariable Long id) {
        return alunoTEService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/alunotes
     * Create new AlunoTE
     * @param alunoTE entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<AlunoTE> create(@RequestBody AlunoTE alunoTE) {
        AlunoTE created = alunoTEService.create(alunoTE);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoTE> update(
            @PathVariable Long id,
            @RequestBody AlunoTE alunoTE) {
        AlunoTE updated = alunoTEService.update(id, alunoTE);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/alunotes/{id}
     * Delete AlunoTE
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!alunoTEService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        alunoTEService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
