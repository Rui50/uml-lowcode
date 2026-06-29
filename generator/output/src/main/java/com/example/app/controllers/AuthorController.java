package com.example.app.controllers;

import com.example.app.models.Author;
import com.example.app.service.AuthorService;
import com.example.app.dto.AuthorCreateDTO;
import com.example.app.dto.AuthorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Author
 */
@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    // CRUD Endpoints

    /**
     * GET /api/authors
     * Retrieve all Author entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<AuthorResponseDTO>> getAll() {
        List<AuthorResponseDTO> entities = authorService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/authors/{id}
     * Retrieve Author by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponseDTO> getById(@PathVariable Long id) {
        return authorService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/authors
     * Create new Author
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<AuthorResponseDTO> create(@RequestBody AuthorCreateDTO dto) {
        AuthorResponseDTO created = authorService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponseDTO> update(
            @PathVariable Long id,
            @RequestBody AuthorCreateDTO dto) {
        AuthorResponseDTO updated = authorService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/authors/{id}
     * Delete Author
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!authorService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        authorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
