package com.example.app.controllers;

import com.example.app.models.Turno;
import com.example.app.service.TurnoService;
import com.example.app.dto.TurnoCreateDTO;
import com.example.app.dto.TurnoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.app.models.Aluno;

/**
 * REST Controller for Turno
 */
@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    // CRUD Endpoints

    /**
     * GET /api/turnos
     * Retrieve all Turno entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<TurnoResponseDTO>> getAll() {
        List<TurnoResponseDTO> entities = turnoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/turnos/{id}
     * Retrieve Turno by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> getById(@PathVariable Long id) {
        return turnoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/turnos
     * Create new Turno
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<TurnoResponseDTO> create(@RequestBody TurnoCreateDTO dto) {
        TurnoResponseDTO created = turnoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> update(
            @PathVariable Long id,
            @RequestBody TurnoCreateDTO dto) {
        TurnoResponseDTO updated = turnoService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/turnos/{id}
     * Delete Turno
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!turnoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        turnoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
