package com.example.app.controllers;

import com.example.app.models.StudentMember;
import com.example.app.service.StudentMemberService;
import com.example.app.dto.StudentMemberCreateDTO;
import com.example.app.dto.StudentMemberResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.LibraryCard;
import com.example.app.models.Loan;
import com.example.app.models.Book;
import com.example.app.models.Library;
import com.example.app.models.Member;
import com.example.app.service.BookService;

/**
 * REST Controller for StudentMember
 */
@RestController
@RequestMapping("/api/studentmembers")
@RequiredArgsConstructor
public class StudentMemberController {

    private final StudentMemberService studentMemberService;
    private final BookService bookService;

    // CRUD Endpoints

    /**
     * GET /api/studentmembers
     * Retrieve all StudentMember entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<Page<StudentMemberResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(studentMemberService.findAll(pageable));
    }

    /**
     * GET /api/studentmembers/{id}
     * Retrieve StudentMember by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentMemberResponseDTO> getById(@PathVariable Long id) {
        return studentMemberService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/studentmembers
     * Create new StudentMember
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<StudentMemberResponseDTO> create(@RequestBody StudentMemberCreateDTO dto) {
        StudentMemberResponseDTO created = studentMemberService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentMemberResponseDTO> update(
            @PathVariable Long id,
            @RequestBody StudentMemberCreateDTO dto) {
        StudentMemberResponseDTO updated = studentMemberService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/studentmembers/{id}
     * Delete StudentMember
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!studentMemberService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        studentMemberService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * ManyToOne: Assign a Library to StudentMember
     */
    @PutMapping("/{id}/library/{libraryId}")
    public ResponseEntity<StudentMemberResponseDTO> assignLibrary(
            @PathVariable Long id,
            @PathVariable Long libraryId) {
        StudentMemberResponseDTO updated = studentMemberService.assignLibrary(id, libraryId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Library from StudentMember
     */
    @DeleteMapping("/{id}/library")
    public ResponseEntity<StudentMemberResponseDTO> removeLibrary(
            @PathVariable Long id) {
        StudentMemberResponseDTO updated = studentMemberService.removeLibrary(id);
        return ResponseEntity.ok(updated);
    }


    /**
     * Add Book entities to StudentMember by IDs
     */
    @PostMapping("/{id}/reservedbook")
    public ResponseEntity<StudentMemberResponseDTO> addReservedBook(
            @PathVariable Long id,
            @RequestBody List<Long> bookIds) {
        StudentMemberResponseDTO updated = studentMemberService.addReservedBook(id, bookIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Book entities from StudentMember by IDs
     * Ex: DELETE /{id}/reservedbook?ids=1,2,3  (ids na query, não no body:
     * um body em DELETE não tem semântica definida e pode ser ignorado por proxies/clientes)
     */
    @DeleteMapping("/{id}/reservedbook")
    public ResponseEntity<StudentMemberResponseDTO> removeReservedBook(
            @PathVariable Long id,
            @RequestParam List<Long> ids) {
        StudentMemberResponseDTO updated = studentMemberService.removeReservedBook(id, ids);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
