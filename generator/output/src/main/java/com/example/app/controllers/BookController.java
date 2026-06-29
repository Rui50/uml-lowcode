package com.example.app.controllers;

import com.example.app.models.Book;
import com.example.app.service.BookService;
import com.example.app.dto.BookCreateDTO;
import com.example.app.dto.BookResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.Loan;
import com.example.app.models.Author;
import com.example.app.models.Member;

/**
 * REST Controller for Book
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // CRUD Endpoints

    /**
     * GET /api/books
     * Retrieve all Book entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<Page<BookResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(bookService.findAll(pageable));
    }

    /**
     * GET /api/books/{id}
     * Retrieve Book by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> getById(@PathVariable Long id) {
        return bookService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/books
     * Create new Book
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<BookResponseDTO> create(@RequestBody BookCreateDTO dto) {
        BookResponseDTO created = bookService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookResponseDTO> update(
            @PathVariable Long id,
            @RequestBody BookCreateDTO dto) {
        BookResponseDTO updated = bookService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/books/{id}
     * Delete Book
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!bookService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * Add Author entities to Book by IDs
     */
    @PostMapping("/{id}/authors")
    public ResponseEntity<BookResponseDTO> addAuthors(
            @PathVariable Long id,
            @RequestBody List<Long> authorIds) {
        BookResponseDTO updated = bookService.addAuthors(id, authorIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Author entities from Book by IDs
     * Ex: DELETE /{id}/authors?ids=1,2,3  (ids na query, não no body:
     * um body em DELETE não tem semântica definida e pode ser ignorado por proxies/clientes)
     */
    @DeleteMapping("/{id}/authors")
    public ResponseEntity<BookResponseDTO> removeAuthors(
            @PathVariable Long id,
            @RequestParam List<Long> ids) {
        BookResponseDTO updated = bookService.removeAuthors(id, ids);
        return ResponseEntity.ok(updated);
    }


    /**
     * ManyToOne: Assign a Member to Book
     */
    @PutMapping("/{id}/borrower/{memberId}")
    public ResponseEntity<BookResponseDTO> assignBorrower(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        BookResponseDTO updated = bookService.assignBorrower(id, memberId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Member from Book
     */
    @DeleteMapping("/{id}/borrower")
    public ResponseEntity<BookResponseDTO> removeBorrower(
            @PathVariable Long id) {
        BookResponseDTO updated = bookService.removeBorrower(id);
        return ResponseEntity.ok(updated);
    }


    /**
     * Add Author entities to Book by IDs
     */
    @PostMapping("/{id}/editors")
    public ResponseEntity<BookResponseDTO> addEditors(
            @PathVariable Long id,
            @RequestBody List<Long> authorIds) {
        BookResponseDTO updated = bookService.addEditors(id, authorIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Author entities from Book by IDs
     * Ex: DELETE /{id}/editors?ids=1,2,3  (ids na query, não no body:
     * um body em DELETE não tem semântica definida e pode ser ignorado por proxies/clientes)
     */
    @DeleteMapping("/{id}/editors")
    public ResponseEntity<BookResponseDTO> removeEditors(
            @PathVariable Long id,
            @RequestParam List<Long> ids) {
        BookResponseDTO updated = bookService.removeEditors(id, ids);
        return ResponseEntity.ok(updated);
    }

}
