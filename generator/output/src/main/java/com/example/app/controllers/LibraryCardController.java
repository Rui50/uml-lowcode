package com.example.app.controllers;

import com.example.app.models.LibraryCard;
import com.example.app.service.LibraryCardService;
import com.example.app.dto.LibraryCardCreateDTO;
import com.example.app.dto.LibraryCardResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.Member;

/**
 * REST Controller for LibraryCard
 */
@RestController
@RequestMapping("/api/librarycards")
@RequiredArgsConstructor
public class LibraryCardController {

    private final LibraryCardService libraryCardService;

    // CRUD Endpoints

    /**
     * GET /api/librarycards
     * Retrieve all LibraryCard entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<LibraryCardResponseDTO>> getAll() {
        List<LibraryCardResponseDTO> entities = libraryCardService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/librarycards/{id}
     * Retrieve LibraryCard by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<LibraryCardResponseDTO> getById(@PathVariable Long id) {
        return libraryCardService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/librarycards
     * Create new LibraryCard
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<LibraryCardResponseDTO> create(@RequestBody LibraryCardCreateDTO dto) {
        LibraryCardResponseDTO created = libraryCardService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibraryCardResponseDTO> update(
            @PathVariable Long id,
            @RequestBody LibraryCardCreateDTO dto) {
        LibraryCardResponseDTO updated = libraryCardService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/librarycards/{id}
     * Delete LibraryCard
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!libraryCardService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        libraryCardService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
