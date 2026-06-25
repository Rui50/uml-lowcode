package com.example.app.controllers;

import com.example.app.models.StaffMember;
import com.example.app.service.StaffMemberService;
import com.example.app.dto.StaffMemberCreateDTO;
import com.example.app.dto.StaffMemberResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * REST Controller for StaffMember
 */
@RestController
@RequestMapping("/api/staffmembers")
@RequiredArgsConstructor
public class StaffMemberController {

    private final StaffMemberService staffMemberService;
    private final BookService bookService;

    // CRUD Endpoints

    /**
     * GET /api/staffmembers
     * Retrieve all StaffMember entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<StaffMemberResponseDTO>> getAll() {
        List<StaffMemberResponseDTO> entities = staffMemberService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/staffmembers/{id}
     * Retrieve StaffMember by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<StaffMemberResponseDTO> getById(@PathVariable Long id) {
        return staffMemberService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/staffmembers
     * Create new StaffMember
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<StaffMemberResponseDTO> create(@RequestBody StaffMemberCreateDTO dto) {
        StaffMemberResponseDTO created = staffMemberService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffMemberResponseDTO> update(
            @PathVariable Long id,
            @RequestBody StaffMemberCreateDTO dto) {
        StaffMemberResponseDTO updated = staffMemberService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/staffmembers/{id}
     * Delete StaffMember
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!staffMemberService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        staffMemberService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * ManyToOne: Assign a Library to StaffMember
     */
    @PutMapping("/{id}/library/{libraryId}")
    public ResponseEntity<StaffMemberResponseDTO> assignLibrary(
            @PathVariable Long id,
            @PathVariable Long libraryId) {
        StaffMemberResponseDTO updated = staffMemberService.assignLibrary(id, libraryId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Library from StaffMember
     */
    @DeleteMapping("/{id}/library")
    public ResponseEntity<StaffMemberResponseDTO> removeLibrary(
            @PathVariable Long id) {
        StaffMemberResponseDTO updated = staffMemberService.removeLibrary(id);
        return ResponseEntity.ok(updated);
    }


    /**
     * Add Book entities to StaffMember by IDs
     */
    @PostMapping("/{id}/reservedbook")
    public ResponseEntity<StaffMemberResponseDTO> addReservedBook(
            @PathVariable Long id,
            @RequestBody List<Long> bookIds) {
        StaffMemberResponseDTO updated = staffMemberService.addReservedBook(id, bookIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Book entities from StaffMember by IDs
     */
    @DeleteMapping("/{id}/reservedbook")
    public ResponseEntity<StaffMemberResponseDTO> removeReservedBook(
            @PathVariable Long id,
            @RequestBody List<Long> bookIds) {
        StaffMemberResponseDTO updated = staffMemberService.removeReservedBook(id, bookIds);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
