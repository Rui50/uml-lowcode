package com.example.app.controllers;

import com.example.app.models.testeSubAb;
import com.example.app.service.testeSubAbService;
import com.example.app.dto.testeSubAbCreateDTO;
import com.example.app.dto.testeSubAbResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for testeSubAb
 */
@RestController
@RequestMapping("/api/testesubabs")
@RequiredArgsConstructor
public class testeSubAbController {

    private final testeSubAbService testeSubAbService;

    // CRUD Endpoints

    /**
     * GET /api/testesubabs
     * Retrieve all testeSubAb entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<testeSubAbResponseDTO>> getAll() {
        List<testeSubAbResponseDTO> entities = testeSubAbService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/testesubabs/{id}
     * Retrieve testeSubAb by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<testeSubAbResponseDTO> getById(@PathVariable Long id) {
        return testeSubAbService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/testesubabs
     * Create new testeSubAb
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<testeSubAbResponseDTO> create(@RequestBody testeSubAbCreateDTO dto) {
        testeSubAbResponseDTO created = testeSubAbService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<testeSubAbResponseDTO> update(
            @PathVariable Long id,
            @RequestBody testeSubAbCreateDTO dto) {
        testeSubAbResponseDTO updated = testeSubAbService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/testesubabs/{id}
     * Delete testeSubAb
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!testeSubAbService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        testeSubAbService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
