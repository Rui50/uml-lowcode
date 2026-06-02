package com.example.app.controllers;

import com.example.app.models.ImplementInterface;
import com.example.app.service.ImplementInterfaceService;
import com.example.app.dto.ImplementInterfaceCreateDTO;
import com.example.app.dto.ImplementInterfaceResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for ImplementInterface
 */
@RestController
@RequestMapping("/api/implementinterfaces")
@RequiredArgsConstructor
public class ImplementInterfaceController {

    private final ImplementInterfaceService implementInterfaceService;

    // CRUD Endpoints

    /**
     * GET /api/implementinterfaces
     * Retrieve all ImplementInterface entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<ImplementInterfaceResponseDTO>> getAll() {
        List<ImplementInterfaceResponseDTO> entities = implementInterfaceService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/implementinterfaces/{id}
     * Retrieve ImplementInterface by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImplementInterfaceResponseDTO> getById(@PathVariable Long id) {
        return implementInterfaceService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/implementinterfaces
     * Create new ImplementInterface
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<ImplementInterfaceResponseDTO> create(@RequestBody ImplementInterfaceCreateDTO dto) {
        ImplementInterfaceResponseDTO created = implementInterfaceService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImplementInterfaceResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ImplementInterfaceCreateDTO dto) {
        ImplementInterfaceResponseDTO updated = implementInterfaceService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/implementinterfaces/{id}
     * Delete ImplementInterface
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!implementInterfaceService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        implementInterfaceService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
