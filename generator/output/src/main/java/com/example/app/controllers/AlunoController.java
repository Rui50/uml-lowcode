package com.example.app.controllers;

import com.example.app.models.Aluno;
import com.example.app.service.AlunoService;
import com.example.app.dto.AlunoCreateDTO;
import com.example.app.dto.AlunoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<List<AlunoResponseDTO>> getAll() {
        List<AlunoResponseDTO> entities = alunoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/alunos/{id}
     * Retrieve Aluno by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> getById(@PathVariable Long id) {
        return alunoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/alunos
     * Create new Aluno
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<AlunoResponseDTO> create(@RequestBody AlunoCreateDTO dto) {
        AlunoResponseDTO created = alunoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> update(
            @PathVariable Long id,
            @RequestBody AlunoCreateDTO dto) {
        AlunoResponseDTO updated = alunoService.update(id, dto);
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

    // Relationship Endpoints

    /**
     * ManyToOne: Assign a Turno to Aluno
     */
    @PutMapping("/{id}/turno/{turnoId}")
    public ResponseEntity<AlunoResponseDTO> assignTurno(
            @PathVariable Long id,
            @PathVariable Long turnoId) {
        AlunoResponseDTO updated = alunoService.assignTurno(id, turnoId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Turno from Aluno
     */
    @DeleteMapping("/{id}/turno")
    public ResponseEntity<AlunoResponseDTO> removeTurno(
            @PathVariable Long id) {
        AlunoResponseDTO updated = alunoService.removeTurno(id);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
