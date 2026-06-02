package com.example.app.controllers;

import com.example.app.models.Turma;
import com.example.app.service.TurmaService;
import com.example.app.dto.TurmaCreateDTO;
import com.example.app.dto.TurmaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Turno;
import com.example.app.models.Aluno;
import com.example.app.service.AlunoService;

/**
 * REST Controller for Turma
 */
@RestController
@RequestMapping("/api/turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final TurmaService turmaService;
    private final AlunoService alunoService;

    // CRUD Endpoints

    /**
     * GET /api/turmas
     * Retrieve all Turma entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<TurmaResponseDTO>> getAll() {
        List<TurmaResponseDTO> entities = turmaService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/turmas/{id}
     * Retrieve Turma by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<TurmaResponseDTO> getById(@PathVariable Long id) {
        return turmaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/turmas
     * Create new Turma
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<TurmaResponseDTO> create(@RequestBody TurmaCreateDTO dto) {
        TurmaResponseDTO created = turmaService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TurmaResponseDTO> update(
            @PathVariable Long id,
            @RequestBody TurmaCreateDTO dto) {
        TurmaResponseDTO updated = turmaService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/turmas/{id}
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

    // Relationship Endpoints

    /**
     * Add Aluno entities to Turma by IDs
     */
    @PostMapping("/{id}/alunos")
    public ResponseEntity<TurmaResponseDTO> addAlunos(
            @PathVariable Long id,
            @RequestBody List<Long> alunoIds) {
        TurmaResponseDTO updated = turmaService.addAlunos(id, alunoIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Aluno entities from Turma by IDs
     */
    @DeleteMapping("/{id}/alunos")
    public ResponseEntity<TurmaResponseDTO> removeAlunos(
            @PathVariable Long id,
            @RequestBody List<Long> alunoIds) {
        TurmaResponseDTO updated = turmaService.removeAlunos(id, alunoIds);
        return ResponseEntity.ok(updated);
    }


    /**
     * Add Turno entities to Turma by IDs
     */
    @PostMapping("/{id}/turnos")
    public ResponseEntity<TurmaResponseDTO> addTurnos(
            @PathVariable Long id,
            @RequestBody List<Long> turnoIds) {
        TurmaResponseDTO updated = turmaService.addTurnos(id, turnoIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Turno entities from Turma by IDs
     */
    @DeleteMapping("/{id}/turnos")
    public ResponseEntity<TurmaResponseDTO> removeTurnos(
            @PathVariable Long id,
            @RequestBody List<Long> turnoIds) {
        TurmaResponseDTO updated = turmaService.removeTurnos(id, turnoIds);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
