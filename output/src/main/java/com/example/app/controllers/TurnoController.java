package com.example.app.controller;

import com.example.app.models.Turno;
import com.example.app.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Turno
 */
@RestController
@RequestMapping("/api/Turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    // ========================================================================
    // CRUD Endpoints
    // Generated from Class Diagram - Fully functional
    // ========================================================================

    /**
     * GET /api/Turnos
     * Retrieve all Turno entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<Turno>> getAll() {
        List<Turno> entities = turnoService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/Turnos/{id}
     * Retrieve Turno by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<Turno> getById(@PathVariable Long id) {
        return turnoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/Turnos
     * Create new Turno
     * @param turno entity data
     * @return created entity with 201 status
     */
    @PostMapping
    public ResponseEntity<Turno> create(@RequestBody Turno turno) {
        Turno saved = turnoService.save(turno);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/Turnos/{id}
     * Update existing Turno
     * @param id entity id
     * @param turno updated data
     * @return updated entity if found, 404 if not
     */
    @PutMapping("/{id}")
    public ResponseEntity<Turno> update(
            @PathVariable Long id,
            @RequestBody Turno turno) {

        if (!turnoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        turno.setId(id);
        Turno updated = turnoService.save(turno);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/Turnos/{id}
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

}
