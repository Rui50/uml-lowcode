package com.example.app.controllers;

import com.example.app.models.AlunoTE;
import com.example.app.service.AlunoTEService;
import com.example.app.dto.AlunoTECreateDTO;
import com.example.app.dto.AlunoTEResponseDTO;
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
    public ResponseEntity<List<AlunoTEResponseDTO>> getAll() {
        List<AlunoTEResponseDTO> entities = alunoTEService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/alunotes/{id}
     * Retrieve AlunoTE by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlunoTEResponseDTO> getById(@PathVariable Long id) {
        return alunoTEService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/alunotes
     * Create new AlunoTE
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<AlunoTEResponseDTO> create(@RequestBody AlunoTECreateDTO dto) {
        AlunoTEResponseDTO created = alunoTEService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoTEResponseDTO> update(
            @PathVariable Long id,
            @RequestBody AlunoTECreateDTO dto) {
        AlunoTEResponseDTO updated = alunoTEService.update(id, dto);
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


    // Custom Operation Endpoints dos diagramas sequencia
}
