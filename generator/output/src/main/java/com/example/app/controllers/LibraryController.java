package com.example.app.controllers;

import com.example.app.models.Library;
import com.example.app.service.LibraryService;
import com.example.app.dto.LibraryCreateDTO;
import com.example.app.dto.LibraryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.example.app.jobs.MarkOverdueJob;

import com.example.app.models.Book;
import com.example.app.models.Member;

/**
 * REST Controller for Library
 */
@RestController
@RequestMapping("/api/librarys")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;
    private final MarkOverdueJob markOverdueJob;

    // CRUD Endpoints

    /**
     * GET /api/librarys
     * Retrieve all Library entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<LibraryResponseDTO>> getAll() {
        List<LibraryResponseDTO> entities = libraryService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/librarys/{id}
     * Retrieve Library by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<LibraryResponseDTO> getById(@PathVariable Long id) {
        return libraryService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/librarys
     * Create new Library
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<LibraryResponseDTO> create(@RequestBody LibraryCreateDTO dto) {
        LibraryResponseDTO created = libraryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibraryResponseDTO> update(
            @PathVariable Long id,
            @RequestBody LibraryCreateDTO dto) {
        LibraryResponseDTO updated = libraryService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/librarys/{id}
     * Delete Library
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!libraryService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        libraryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * Add Book entities to Library by IDs
     */
    @PostMapping("/{id}/catalog")
    public ResponseEntity<LibraryResponseDTO> addCatalog(
            @PathVariable Long id,
            @RequestBody List<Long> bookIds) {
        LibraryResponseDTO updated = libraryService.addCatalog(id, bookIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Book entities from Library by IDs
     */
    @DeleteMapping("/{id}/catalog")
    public ResponseEntity<LibraryResponseDTO> removeCatalog(
            @PathVariable Long id,
            @RequestBody List<Long> bookIds) {
        LibraryResponseDTO updated = libraryService.removeCatalog(id, bookIds);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia

    @PostMapping("/markOverdue")
    public ResponseEntity<?> markOverdue() {
        markOverdueJob.run();
        return ResponseEntity.ok().build();
    }
}
